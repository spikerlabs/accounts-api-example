package com.spikerlabs.accounts.service

sealed trait Error extends Response {
  def message: String
}

object Error {
  case class NotFound(message: String = "") extends Error
}
