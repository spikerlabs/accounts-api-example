package com.spikerlabs.accounts

import com.spikerlabs.accounts.aggregate.Account
import com.spikerlabs.accounts.service.{Request, Response, Error}
import com.spikerlabs.accounts.storage.Storage
import monix.eval.Task

/**
  * Domain service: abstracts the domain logic from specific library, which will handle HTTP API
  * @param storage the storage implementation
  */
case class Service(private val storage: Storage) {

  /**
    * Handles all implemented requests according to business rules
    * @param request abstraction of a service request (currently supported transfer, create an account, deposit funds
    * @return either response or an error
    */
  def handle(request: Request): Task[Response] = request match {
    case request : Request.Transfer => transfer(request)
    case _ : Request.CreateAccount => createAccount
    case request : Request.Deposit => deposit(request)
  }

  /**
    * Transfers the amount between two accounts
    */
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

  /**
    * Creates the account and returns it's details
    */
  private def createAccount = {
    val newAccount = Account()
    storage.storeAccount(newAccount).map(_ => Response.Account(newAccount.id, newAccount.balance))
  }

  /**
    * Deposit funds to the account and returns details of updated account
    */
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
