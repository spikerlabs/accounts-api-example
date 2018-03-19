package com.spikerlabs.accounts

import com.spikerlabs.accounts.service.{Error, Request, Response}
import com.spikerlabs.accounts.storage.Storage
import io.circe.syntax._
import io.circe.generic.auto._
import monix.eval.Task
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

case class Api(private val storage: Storage) extends Http4sDsl[Task] {

  val service = Service(storage)
  val httpService: HttpService[Task] = HttpService[Task] {
    case request @ POST -> Root / "transfer" =>
      request.decodeJson[Request.Transfer] flatMap {
        service.handle(_).flatMap {
          case _: Response.SuccessfulTransfer => Ok()
          case response: Error.NotFound => NotFound(response.asJson)
        }
      }
    case POST -> Root / "create" =>
      service.handle(Request.CreateAccount()).flatMap {
        case response: Response.Account => Ok(response.asJson)
      }
    case request @ POST -> Root / "deposit" =>
      request.decodeJson[Request.Deposit] flatMap {
        service.handle(_).flatMap {
          case response: Response.Account => Ok(response.asJson)
          case response: Error.NotFound => NotFound(response.asJson)
        }
      }
  }

}
