package com.spikerlabs.accounts.service

import com.spikerlabs.accounts.domain.{AccountID, Money}

sealed trait Request {
}

object Request {
  case class CreateAccount() extends Request
  case class Transfer(source: AccountID, destination: AccountID, funds: Money) extends Request
  case class Deposit(account: AccountID, funds: Money) extends Request
}
