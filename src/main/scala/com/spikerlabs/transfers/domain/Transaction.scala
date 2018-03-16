package com.spikerlabs.transfers.domain

sealed trait Transaction {
  def amount: Money
}

object Transaction {
  case class Deposit(destination: AccountID, amount: Money) extends Transaction
  case class Withdrawal(source: AccountID, amount: Money) extends Transaction
  trait Transfer extends Transaction
  case class TransferOut(source: AccountID, destination: AccountID, amount: Money) extends Transfer
  case class TransferIn(source: AccountID, destination: AccountID, amount: Money) extends Transfer
}
