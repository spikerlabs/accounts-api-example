package com.spikerlabs.accounts

import com.spikerlabs.accounts.service.{Error, Request, Response}
import io.circe.syntax._
import io.circe.generic.auto._
import monix.eval.Task
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder

import monix.execution.Scheduler.Implicits.global

object Api extends Http4sDsl[Task] {
  def httpService(service: Service): HttpService[Task] = HttpService[Task] {
    case request @ POST -> Root / "transfer" =>
      request.decodeJson[Request.Transfer] flatMap {
        service.handle(_).flatMap {
          case _: Response.SuccessfulTransfer => Ok()
          case response: Error.NotFound => NotFound(response.asJson)
          case response: Error.InvalidTransfer => BadRequest(response.asJson)
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
    case GET -> Root / "healthcheck" => Ok()
  }

  def httpStream(service: Service, port: Int = 8080, host: String = "0.0.0.0"): BlazeBuilder[Task] =
    BlazeBuilder[Task].bindHttp(port, host).mountService(httpService(service), "/")
}
