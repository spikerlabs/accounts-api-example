package com.spikerlabs.accounts

import java.util.UUID

import com.spikerlabs.accounts.aggregate.Account
import com.spikerlabs.accounts.domain.{AccountID, Money}
import com.spikerlabs.accounts.service.{Request, Response, Error}
import com.spikerlabs.accounts.storage.InMemoryStorage
import org.scalatest._
import org.scalatest.Matchers._
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.Await
import scala.concurrent.duration._

class ServiceSpec extends AsyncFlatSpec {

  "Service" should "execute a valid transfer between two valid accounts" in {
    val storage = InMemoryStorage()
    val sourceAccount = Account().deposit(Money(300))
    val destinationAccount = Account()
    Await.ready(storage.storeAccount(sourceAccount).runAsync, 1.second)
    Await.ready(storage.storeAccount(destinationAccount).runAsync, 1.second)
    val service = Service(storage)

    val request = Request.Transfer(sourceAccount.id, destinationAccount.id, Money(100))

    service.handle(request).flatMap { response =>
      assert(response == Response.SuccessfulTransfer())
      storage.findAccount(sourceAccount.id).map((response, _))
    }.flatMap {
      case (response, maybeSourceAccount) =>
        assert(maybeSourceAccount.get.balance == Money(200))
        storage.findAccount(destinationAccount.id).map((response, maybeSourceAccount, _))
    }.map {
      case (response, maybeSourceAccount, maybeDestinationAccount) =>
        assert(maybeDestinationAccount.get.balance == Money(100))
    }.runAsync
  }

  it should "return error if transfer amount is negative" in {
    val storage = InMemoryStorage()
    val sourceAccount = Account().deposit(Money(300))
    val destinationAccount = Account()
    Await.ready(storage.storeAccount(sourceAccount).runAsync, 1.second)
    Await.ready(storage.storeAccount(destinationAccount).runAsync, 1.second)
    val service = Service(storage)

    val request = Request.Transfer(sourceAccount.id, destinationAccount.id, Money(-100))

    service.handle(request).map { response =>
      assert(response == Error.InvalidTransfer())
    }.runAsync
  }

  it should "return error if source account was not found" in {
    val storage = InMemoryStorage()
    val service = Service(storage)

    val request = Request.Transfer(AccountID(UUID.randomUUID()), AccountID(UUID.randomUUID()), Money(100))

    service.handle(request).map { response =>
        assert(response == Error.NotFound("source account was not found"))
    }.runAsync
  }

  it should "return error if destination account was not found" in {
    val storage = InMemoryStorage()
    val sourceAccount = Account().deposit(Money(300))
    Await.ready(storage.storeAccount(sourceAccount).runAsync, 1.second)
    val service = Service(storage)

    val request = Request.Transfer(sourceAccount.id, AccountID(UUID.randomUUID()), Money(100))

    service.handle(request).map { response =>
        assert(response == Error.NotFound("destination account was not found"))
    }.runAsync
  }

  it should "create a new account and return it's id" in {
    val storage = InMemoryStorage()
    val service = Service(storage)

    service.handle(Request.CreateAccount()).flatMap { response =>
      response shouldBe a [Response.Account]
      storage.findAccount(response.asInstanceOf[Response.Account].account).map((response, _))
    }.map {
      case (response, maybeNewAccount) =>
        assert(maybeNewAccount.isDefined)
        assert(maybeNewAccount.get.balance == Money(0))
    }.runAsync
  }

  it should "deposit funds to an existing account" in {
    val storage = InMemoryStorage()
    val account = Account()
    Await.ready(storage.storeAccount(account).runAsync, 1.second)
    val service = Service(storage)

    val request = Request.Deposit(account.id, Money(100))

    service.handle(request).flatMap { response =>
      response shouldBe a [Response.Account]
      storage.findAccount(account.id).map((response, _))
    }.map {
      case (response, maybeUpdatedAccount) =>
        assert(maybeUpdatedAccount.get.id == account.id)
        assert(maybeUpdatedAccount.get.balance == Money(100))
    }.runAsync
  }

  it should "return an error when attempting to deposit funds to not existing account" in {
    val storage = InMemoryStorage()
    val service = Service(storage)

    val request = Request.Deposit(AccountID(UUID.randomUUID()), Money(100))

    service.handle(request).map { response =>
      assert(response == Error.NotFound("account was not found"))
    }.runAsync
  }

}
