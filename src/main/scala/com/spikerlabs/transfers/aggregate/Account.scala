package com.spikerlabs.transfers.aggregate

import java.util.UUID

import com.spikerlabs.transfers.domain.Transaction._
import com.spikerlabs.transfers.domain.{AccountID, Money, Transaction}

case class Account private(id: AccountID, private val transactions: List[Transaction]) {

  lazy val balance: Money = transactions.foldRight(Money(0)) {
    (transaction: Transaction, balanceSoFar: Money) =>
      transaction match {
        case Deposit(_, Money(depositAmount)) => Money(balanceSoFar.amount + depositAmount)
        case Withdrawal(_, Money(withdrawalAmount)) => Money(balanceSoFar.amount - withdrawalAmount)
        case Transfer(_, this.id, Money(transferAmount)) => Money(balanceSoFar.amount + transferAmount)
        case Transfer(this.id, _, Money(transferAmount)) => Money(balanceSoFar.amount - transferAmount)
      }
  }

  def deposit(amount: Money): Account = Account(id, transactions :+ Deposit(id, amount))

  def withdraw(amount: Money): Account = Account(id, transactions :+ Withdrawal(id, amount))

  def transferOut(amount: Money, destination: Account): (Account, Account) = {
    val transfer = Transfer(this.id, destination.id, amount)
    (Account(this.id, this.transactions :+ transfer), Account(destination.id, destination.transactions :+ transfer))
  }

}

object Account {

  def apply(): Account = {
    val id = AccountID(UUID.randomUUID())
    Account(id, List(Deposit(id, Money(0))))
  }

  def apply(transaction: Transaction): Option[Account] = apply(List(transaction))

  def apply(transaction: List[Transaction]): Option[Account] = {
    transaction match {
      case initialTransactions @ Deposit(id, _) :: _ => Some(Account(id, initialTransactions))
      case _ => None
    }
  }

}
