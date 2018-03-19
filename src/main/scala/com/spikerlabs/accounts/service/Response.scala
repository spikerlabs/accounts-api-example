package com.spikerlabs.accounts.service

import com.spikerlabs.accounts.domain.{AccountID, Money}

trait Response {
}

object Response {
  case class SuccessfulTransfer() extends Response
  case class Account(account: AccountID, balance: Money) extends Response
}
