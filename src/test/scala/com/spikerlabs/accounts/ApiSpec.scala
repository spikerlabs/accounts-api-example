package com.spikerlabs.accounts

import java.util.UUID

import io.circe.generic.auto._
import io.circe.parser.decode
import com.spikerlabs.accounts.aggregate.Account
import com.spikerlabs.accounts.domain.Money
import com.spikerlabs.accounts.service.Response
import com.spikerlabs.accounts.storage.InMemoryStorage
import monix.eval.Task
import org.scalatest._
import org.scalatest.Matchers._
import monix.execution.Scheduler.Implicits.global
import org.http4s.{EntityBody, Method, Request, Status, Uri}
import fs2.Stream
import fs2.text.utf8Encode

import scala.concurrent.Await
import scala.concurrent.duration._

class ApiSpec extends AsyncFlatSpec {

  val storage = InMemoryStorage()
  val httpService = Api.httpService(Service(storage))

  def parseBody(body: EntityBody[Task]): String = Await.result(body.compile.fold("")(_ + _.toChar).runAsync, 1.second)

  def makeBody(body: String): EntityBody[Task] = Stream(body).through(utf8Encode)

  "HttpService" should "transfer the funds" in {
    val oneAccount = Account().deposit(Money(500))
    val otherAccount = Account()
    Await.ready(storage.storeAccount(oneAccount, otherAccount).runAsync, 1.second)
    val request = Request[Task](
      Method.POST,
      Uri.uri("/transfer"),
      body = makeBody(s"""{"source": {"id": "${oneAccount.id.id.toString}"}, "destination": {"id": "${otherAccount.id.id.toString}"}, "funds": {"amount": 50}}""")
    )

    httpService.run(request).value.flatMap { maybeResponse =>
      assert(maybeResponse.isDefined)
      assert(maybeResponse.get.status == Status.Ok)
      storage.findAccount(oneAccount.id).map((maybeResponse.get, _))
    }.flatMap {
      case (response, maybeSourceAccount) =>
        assert(maybeSourceAccount.get.balance == Money(450))
        storage.findAccount(otherAccount.id).map((response, maybeSourceAccount, _))
    }.map {
      case (response, maybeSourceAccount, maybeDestinationAccount) =>
        assert(maybeDestinationAccount.get.balance == Money(50))
    }.runAsync
  }

  it should "return 404 when at least one of transfer accounts was not found" in {
    val account = Account().deposit(Money(20))
    Await.ready(storage.storeAccount(account).runAsync, 1.second)
    val request = Request[Task](
      Method.POST,
      Uri.uri("/transfer"),
      body = makeBody(s"""{"source": {"id": "${account.id.id.toString}"}, "destination": {"id": "${UUID.randomUUID()}"}, "funds": {"amount": 50}}""")
    )

    httpService.run(request).value.map { maybeResponse =>
      assert(maybeResponse.isDefined)
      assert(maybeResponse.get.status == Status.NotFound)
    }.runAsync
  }

  it should "return client error when transfer amount is negative" in {
    val oneAccount = Account().deposit(Money(20))
    val otherAccount = Account()
    Await.ready(storage.storeAccount(oneAccount, otherAccount).runAsync, 1.second)
    val request = Request[Task](
      Method.POST,
      Uri.uri("/transfer"),
      body = makeBody(s"""{"source": {"id": "${oneAccount.id.id.toString}"}, "destination": {"id": "${otherAccount.id.id.toString}"}, "funds": {"amount": -50}}""")
    )

    httpService.run(request).value.map { maybeResponse =>
      assert(maybeResponse.isDefined)
      assert(maybeResponse.get.status == Status.BadRequest)
    }.runAsync
  }

  it should "return client error when source and deposit are the same" in {
    val oneAccount = Account().deposit(Money(20))
    Await.ready(storage.storeAccount(oneAccount).runAsync, 1.second)
    val request = Request[Task](
      Method.POST,
      Uri.uri("/transfer"),
      body = makeBody(s"""{"source": {"id": "${oneAccount.id.id.toString}"}, "destination": {"id": "${oneAccount.id.id.toString}"}, "funds": {"amount": 50}}""")
    )

    httpService.run(request).value.map { maybeResponse =>
      assert(maybeResponse.isDefined)
      assert(maybeResponse.get.status == Status.BadRequest)
    }.runAsync
  }

  it should "create an account" in {
    val request = Request[Task](Method.POST, Uri.uri("/create"))
    httpService.run(request).value.map { maybeResponse =>
      assert(maybeResponse.isDefined)
      assert(maybeResponse.get.status == Status.Ok)
      val body = parseBody(maybeResponse.get.body)
      val eitherResponse = decode[Response.Account](body)
      assert(eitherResponse.isRight)
      eitherResponse.foreach { response =>
        response shouldBe a[Response.Account]
        val maybeAccount = Await.result(storage.findAccount(response.account).runAsync, 1.second)
        assert(maybeAccount.isDefined)
        assert(maybeAccount.get.id == response.account)
      }
      succeed
    }.runAsync
  }

  it should "deposit an account" in {
    val account = Account().deposit(Money(100))
    Await.ready(storage.storeAccount(account).runAsync, 1.second)
    val request = Request[Task](
      Method.POST,
      Uri.uri("/deposit"),
      body = makeBody(s"""{"account": {"id": "${account.id.id.toString}"}, "funds": {"amount": 200}}""")
    )
    httpService.run(request).value.map { maybeResponse =>
      assert(maybeResponse.isDefined)
      assert(maybeResponse.get.status == Status.Ok)
      val body = parseBody(maybeResponse.get.body)
      val eitherResponse = decode[Response.Account](body)
      assert(eitherResponse.isRight)
      eitherResponse.foreach { response =>
        response shouldBe a[Response.Account]
        val maybeAccount = Await.result(storage.findAccount(response.account).runAsync, 1.second)
        assert(maybeAccount.isDefined)
        assert(maybeAccount.get.id == response.account)
        assert(maybeAccount.get.balance == Money(300))
      }
      succeed
    }.runAsync
  }

  it should "return 404 when deposit account was not found" in {
    val request = Request[Task](
      Method.POST,
      Uri.uri("/deposit"),
      body = makeBody(s"""{"account": {"id": "${UUID.randomUUID()}"}, "funds": {"amount": 200}}""")
    )
    httpService.run(request).value.map { maybeResponse =>
      assert(maybeResponse.isDefined)
      assert(maybeResponse.get.status == Status.NotFound)
    }.runAsync
  }

  it should "return success on healthcheck" in {
    val request = Request[Task](Method.GET, Uri.uri("/healthcheck"))
    httpService.run(request).value.map { maybeResponse =>
      assert(maybeResponse.isDefined)
      assert(maybeResponse.get.status == Status.Ok)
    }.runAsync
  }

}
