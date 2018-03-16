package com.spikerlabs.transfers.aggregate

import java.util.UUID

import com.spikerlabs.transfers.domain.Transaction._
import com.spikerlabs.transfers.domain.{AccountID, Money}
import org.scalatest._

class AccountSpec  extends FlatSpec {

  "Account" should "be with zero balance when no transactions passed during construction" in {
    val account = Account()
    assert(account.balance == Money(0))
  }

  it should "assume account id and balance from single deposit passed during construction" in {
    val initialDeposit = Deposit(AccountID(UUID.randomUUID()), Money(100))
    val maybeAccount = Account(initialDeposit)
    assert(maybeAccount.isDefined)
    maybeAccount.foreach { account =>
      assert(account.id == initialDeposit.destination)
      assert(account.balance == Money(100))
    }
  }

  it should "be constructed with list of transactions" in {
    val id = AccountID(UUID.randomUUID())
    val transactions = List(Deposit(id, Money(100)), Deposit(id, Money(100)))
    val maybeAccount = Account(transactions)
    assert(maybeAccount.isDefined)
    maybeAccount.foreach { account =>
      assert(account.id == transactions.head.destination)
      assert(account.balance == Money(200))
    }
  }

  it should "return none when first transaction is not a deposit" in {
    val transaction = Withdrawal(AccountID(UUID.randomUUID()), Money(200))
    assert(Account(transaction).isEmpty)
  }

  it should "calculate the balance of an account lazily from deposits,  withdrawals and transfers in and out" in {
    val id = AccountID(UUID.randomUUID())
    val transactions = List(
      Deposit(id, Money(10)),                                     // 10
      Deposit(id, Money(30)),                                     // 40
      Withdrawal(id, Money(20)),                                  // 20
      TransferOut(id, AccountID(UUID.randomUUID()), Money(5)),    // 15
      TransferIn(id, AccountID(UUID.randomUUID()), Money(2.99))   // 17.99
    )
    val maybeAccount = Account(transactions)
    assert(maybeAccount.isDefined)
    maybeAccount.foreach { account =>
      assert(account.balance == Money(17.99))
    }
  }

  it should "return a copy of account with deposited amount" in {
    val accountBefore = Account()
    val accountAfter = accountBefore.deposit(Money(20))
    assert(accountAfter.id == accountBefore.id)
    assert(accountAfter.balance == Money(20))
  }

  it should "return a copy of account with withdrawn amount" in {
    val accountBefore = Account()
    val accountAfter = accountBefore.deposit(Money(20)).withdraw(Money(10))
    assert(accountAfter.id == accountBefore.id)
  }

  it should "return a pair of updated account after a transfer out of account" in {
    val oneAccount = Account().deposit(Money(100))
    val anotherAccount = Account()
    val (oneAccountAfter, anotherAccountAfter) = oneAccount.transferOut(Money(50), anotherAccount)
    assert(oneAccountAfter.balance == Money(50))
    assert(anotherAccountAfter.balance == Money(50))
  }

  it should "return a pair of updated accounts after a transfer into account" in {
    val oneAccount = Account().deposit(Money(100))
    val anotherAccount = Account().deposit(Money(300))
    val (oneAccountAfter, anotherAccountAfter) = oneAccount.transferIn(Money(150), anotherAccount)
    assert(oneAccountAfter.balance == Money(250))
    assert(anotherAccountAfter.balance == Money(150))
  }
}
