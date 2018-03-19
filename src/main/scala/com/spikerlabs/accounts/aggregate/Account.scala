package com.spikerlabs.accounts.aggregate

import java.util.UUID

import com.spikerlabs.accounts.domain.Transaction._
import com.spikerlabs.accounts.domain.{AccountID, Money, Transaction, Account => DomainAccount}

case class Account private(id: AccountID, private val transactions: List[Transaction]) extends DomainAccount {

  lazy val balance: Money = transactions.foldRight(Money(0)) {
    (transaction: Transaction, balanceSoFar: Money) =>
      transaction match {
        case Deposit(_, Money(depositAmount), _) => Money(balanceSoFar.amount + depositAmount)
        case Withdrawal(_, Money(withdrawalAmount), _) => Money(balanceSoFar.amount - withdrawalAmount)
        case TransferOut(this.id, _, Money(transferAmount), _) => Money(balanceSoFar.amount - transferAmount)
        case TransferIn(this.id, _, Money(transferAmount), _) => Money(balanceSoFar.amount + transferAmount)
      }
  }

  def deposit(amount: Money): Account = Account(id, transactions :+ Deposit(id, amount))

  def withdraw(amount: Money): Account = Account(id, transactions :+ Withdrawal(id, amount))

  def transferOut(amount: Money, destination: Account): (Account, Account) = (
    Account(this.id, this.transactions :+ TransferOut(this.id, destination.id, amount)),
    Account(destination.id, destination.transactions :+ TransferIn(destination.id, this.id, amount))
  )

  def transferIn(amount: Money, source: Account): (Account, Account) = source.transferOut(amount, this).swap

}

object Account {

  def apply(): Account = {
    val id = AccountID(UUID.randomUUID())
    Account(id, List(Deposit(id, Money(0))))
  }

  def apply(transaction: Transaction): Option[Account] = apply(List(transaction))

  def apply(transaction: List[Transaction]): Option[Account] = {
    transaction match {
      case initialTransactions @ Deposit(id, _, _) :: _ => Some(Account(id, initialTransactions))
      case _ => None
    }
  }

}
