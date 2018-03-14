package com.spikerlabs.transfers.aggregate

import org.scalatest._

class AccountSpec  extends FlatSpec {
  "Account" should "be with zero balance when no transactions passed during construction" in {
    val account = Account()
    assert(account.balance == Money(0))
  }
}
