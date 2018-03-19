package com.spikerlabs.accounts.storage

import com.spikerlabs.accounts.domain.{AccountID, Transaction}
import com.spikerlabs.accounts.domain.Transaction.{Deposit, TransferIn, TransferOut, Withdrawal}
import monix.eval.Task

import scala.collection.mutable

case class InMemoryStorage() extends Storage {
  private val transactions: mutable.MutableList[Transaction] = mutable.MutableList.empty

  protected def findTransactions(id: AccountID): Task[List[Transaction]] = Task.eval {
    transactions.filter {
      case Deposit(`id`, _, _) => true
      case Withdrawal(`id`, _, _) => true
      case TransferIn(`id`, _, _, _) => true
      case TransferOut(`id`, _, _, _) => true
      case _ => false
    }.toList
  }

  protected def storeTransactions(transactions: List[Transaction]): Task[Unit] = Task.eval {
    val newTransactions = transactions diff this.transactions
    this.transactions ++= newTransactions
  }
}
