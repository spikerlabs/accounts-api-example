package com.spikerlabs.transfers.domain

sealed trait Transaction {
  def amount: Money
}

object Transaction {
  case class Deposit(destination: AccountID, amount: Money) extends Transaction
  case class Withdrawal(source: AccountID, amount: Money) extends Transaction
  case class Transfer(source: AccountID, destination: AccountID, amount: Money) extends Transaction
}