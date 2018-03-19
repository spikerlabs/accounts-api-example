package com.spikerlabs.accounts

import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser.decode
import com.spikerlabs.accounts.aggregate.Account
import com.spikerlabs.accounts.domain.Money
import com.spikerlabs.accounts.service.Response
import com.spikerlabs.accounts.storage.InMemoryStorage
import monix.eval.Task
import org.scalatest._
import org.scalatest.Matchers._
import monix.execution.Scheduler.Implicits.global
import org.http4s.{Method, Request, Status, Uri}
import io.circe.fs2._

import scala.concurrent.Await
import scala.concurrent.duration._

class ApiSpec extends AsyncFlatSpec {

  val storage = InMemoryStorage()
  val api = Api.httpService(Service(storage))

  it should "create an account" in {
    val request = Request[Task](Method.POST, Uri.uri("/create"))
    api.run(request).value.map { maybeResponse =>
      assert(maybeResponse.isDefined)
      assert(maybeResponse.get.status == Status.Ok)
      val body = Await.result(maybeResponse.get.body.compile.fold("")(_ + _.toChar).runAsync, 1.second)
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

}
