package com.spikerlabs.accounts.domain

import java.util.UUID

sealed trait Transaction {
  def amount: Money
  def id: TransactionID
}

object Transaction {
  case class Deposit(destination: AccountID, amount: Money, id: TransactionID = TransactionID(UUID.randomUUID())) extends Transaction
  case class Withdrawal(source: AccountID, amount: Money, id: TransactionID = TransactionID(UUID.randomUUID())) extends Transaction
  sealed trait Transfer extends Transaction
  case class TransferOut(source: AccountID, destination: AccountID, amount: Money, id: TransactionID = TransactionID(UUID.randomUUID())) extends Transfer
  case class TransferIn(source: AccountID, destination: AccountID, amount: Money, id: TransactionID = TransactionID(UUID.randomUUID())) extends Transfer
}
