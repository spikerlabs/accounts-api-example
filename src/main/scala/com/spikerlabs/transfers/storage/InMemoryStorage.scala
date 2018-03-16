package com.spikerlabs.transfers.storage

import com.spikerlabs.transfers.domain.{AccountID, Transaction}
import com.spikerlabs.transfers.domain.Transaction.{Deposit, TransferIn, TransferOut, Withdrawal}
import monix.eval.Task

case class InMemoryStorage() extends Storage {
  private var transactions: List[Transaction] = List.empty

  def findTransactions(id: AccountID): Task[List[Transaction]] = Task.eval {
    transactions.filter {
      case Deposit(`id`, _) => true
      case Withdrawal(`id`, _) => true
      case TransferIn(`id`, _, _) => true
      case TransferOut(`id`, _, _) => true
      case _ => false
    }
  }

  def storeTransactions(transactions: List[Transaction]): Task[Unit] = Task.eval {
    this.transactions = (this.transactions diff transactions) ++ transactions
  }
}
