package com.spikerlabs.accounts.storage

import com.spikerlabs.accounts.aggregate.Account
import com.spikerlabs.accounts.domain.{AccountID, Transaction}
import monix.eval.Task

trait Storage {
  def findAccount(id: AccountID): Task[Option[Account]] = findTransactions(id).map(Account(_))

  def storeAccount(account: Account): Task[Unit] = account match {
    case Account(_, transactions) => storeTransactions(transactions)
  }

  def storeAccount(accounts: Account*): Task[Unit] = Task.gatherUnordered {
    accounts.map {
      case Account(_, transactions) => storeTransactions(transactions)
    }
  }.map(_.reduce((_, _) => ()))

  def findTransactions(id: AccountID): Task[List[Transaction]]
  def storeTransactions(transactions: List[Transaction]): Task[Unit]
}
