package com.spikerlabs.accounts

import com.spikerlabs.accounts.aggregate.Account
import com.spikerlabs.accounts.service.{Request, Response, Error}
import com.spikerlabs.accounts.storage.Storage
import monix.eval.Task

/**
  * Domain service: abstracts the domain logic from specific http library
  * @param storage the storage implementation
  */
case class Service(private val storage: Storage) {

  def handle(request: Request): Task[Response] = request match {
    case request : Request.Transfer =>
      if (request.funds.amount > 0 && request.source != request.destination) transfer(request)
      else Task.eval(Error.InvalidTransfer())
    case _ : Request.CreateAccount => createAccount
    case request : Request.Deposit => deposit(request)
  }

  private def transfer(request: Request.Transfer) = {
    storage.findAccount(request.source).flatMap {
      case None => Task.eval(Error.NotFound("source account was not found"))
      case Some(sourceAccount) => storage.findAccount(request.destination).flatMap {
        case None => Task.eval(Error.NotFound("destination account was not found"))
        case Some(destinationAccount) =>
          val (updatedSourceAccount, updatedDestinationAccount) = sourceAccount.transferOut(request.funds, destinationAccount)
          storage.storeAccount(updatedSourceAccount, updatedDestinationAccount)
            .map(_ => Response.SuccessfulTransfer())
      }
    }
  }

  private def createAccount = {
    val newAccount = Account()
    storage.storeAccount(newAccount).map(_ => Response.Account(newAccount.id, newAccount.balance))
  }

  private def deposit(request: Request.Deposit) = {
    storage.findAccount(request.account).flatMap {
      case None => Task.eval(Error.NotFound("account was not found"))
      case Some(account) =>
        val updatedAccount = account.deposit(request.funds)
        storage.storeAccount(updatedAccount)
          .map(_ => Response.Account(updatedAccount.id, updatedAccount.balance))
    }
  }

}
