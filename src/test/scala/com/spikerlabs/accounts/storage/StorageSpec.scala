package com.spikerlabs.accounts.storage

import java.util.UUID

import com.spikerlabs.accounts.aggregate.Account
import com.spikerlabs.accounts.domain.{AccountID, Money}
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalatest._
import org.scalatest.Matchers._

import scala.concurrent.Await
import scala.concurrent.duration._

class StorageSpec extends AsyncFlatSpec {

  "InMemoryStorage" should "implement storage trait" in {
    InMemoryStorage() shouldBe a [Storage]
  }

  it should "return none for not existing account" in {
    val storage = InMemoryStorage()

    storage.findAccount(AccountID(UUID.randomUUID())).map { maybeAccount =>
      assert(maybeAccount.isEmpty)
    }.runAsync
  }

  it should "return an account, which was just stored" in {
    val accountToStorage = Account()
    val storage = InMemoryStorage()
    Await.ready(storage.storeAccount(accountToStorage).runAsync, 1.second)
    storage.findAccount(accountToStorage.id).map { maybeAccount =>
      assert(maybeAccount.isDefined)
      assert(maybeAccount.get.balance == Money(0))
      assert(maybeAccount.get == accountToStorage)
    }.runAsync
  }

  it should "store a single account with number of transactions associated to it" in {
    val anotherAccount = Account().deposit(Money(500))
    val accountToStore = Account()
      .deposit(Money(100))                        // 100
      .deposit(Money(100))                        // 200
      .withdraw(Money(50))                        // 150
      .transferIn(Money(70), anotherAccount)._1   // 220
      .transferOut(Money(100), anotherAccount)._1 // 120
    val storage = InMemoryStorage()
    Await.ready(storage.storeAccount(accountToStore).runAsync, 1.second)
    storage.findAccount(accountToStore.id).map { maybeAccount =>
      assert(maybeAccount.isDefined)
      assert(maybeAccount.get.balance == Money(120))
      assert(maybeAccount.get == accountToStore)
    }.runAsync
  }

  it should "store multiple accounts with not transactions" in {
    val oneAccount = Account().deposit(Money(500)).withdraw(Money(150))                                 // 350
    val anotherAccount = Account().deposit(Money(100)).withdraw(Money(20))                              // 80
    val (oneAccountToStore, anotherAccountToStore) = oneAccount.transferOut(Money(200), anotherAccount) // 150, 280
    val storage = InMemoryStorage()
    Await.ready(storage.storeAccount(oneAccountToStore, anotherAccountToStore).runAsync, 1.second)

    val findBothAccounts: Task[(Option[Account], Option[Account])] = for {
      maybeOneAccount <- storage.findAccount(oneAccount.id)
      maybeAnotherAccount <- storage.findAccount(anotherAccount.id)
    } yield (maybeOneAccount, maybeAnotherAccount)

    findBothAccounts.map {
      case (maybeOneAccount, maybeAnotherAccount) =>
        assert(maybeOneAccount.isDefined)
        assert(maybeOneAccount.get.balance == Money(150))
        assert(maybeOneAccount.get == oneAccountToStore)
        assert(maybeAnotherAccount.isDefined)
        assert(maybeAnotherAccount.get.balance == Money(280))
        assert(maybeAnotherAccount.get == anotherAccountToStore)
    }.runAsync
  }

  it should "not handle repeated saves of the objects" in {
    val oneAccount = Account().deposit(Money(500)).withdraw(Money(150))     // 350
    val anotherAccount = Account().deposit(Money(100)).withdraw(Money(20))  // 80
    val storage = InMemoryStorage()
    Await.ready(storage.storeAccount(oneAccount).runAsync, 1.second)
    Await.ready(storage.storeAccount(oneAccount).runAsync, 1.second)
    val updatedOneAccount = oneAccount.deposit(Money(40))           // 390
    val updatedAnotherAccount = anotherAccount.withdraw(Money(40))  // 40
    Await.ready(storage.storeAccount(updatedOneAccount).runAsync, 1.second)
    Await.ready(storage.storeAccount(updatedOneAccount).runAsync, 1.second)
    Await.ready(storage.storeAccount(updatedAnotherAccount).runAsync, 1.second)

    val findBothAccounts: Task[(Option[Account], Option[Account])] = for {
      maybeOneAccount <- storage.findAccount(oneAccount.id)
      maybeAnotherAccount <- storage.findAccount(anotherAccount.id)
    } yield (maybeOneAccount, maybeAnotherAccount)

    findBothAccounts.map {
      case (maybeOneAccount, maybeAnotherAccount) =>
        assert(maybeOneAccount.isDefined)
        assert(maybeOneAccount.get.balance == Money(390))
        assert(maybeOneAccount.get == updatedOneAccount)
        assert(maybeAnotherAccount.isDefined)
        assert(maybeAnotherAccount.get.balance == Money(40))
        assert(maybeAnotherAccount.get == updatedAnotherAccount)
    }.runAsync
  }

}
