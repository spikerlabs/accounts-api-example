package com.spikerlabs.accounts.service

sealed trait Error extends Response

object Error {
  case class NotFound(message: String = "") extends Error
  case class InvalidTransfer() extends Error
}
