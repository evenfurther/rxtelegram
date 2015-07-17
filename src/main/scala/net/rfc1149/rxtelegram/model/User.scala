package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Reads, Json, Format}

case class User(id: Int, first_name: String, last_name: Option[String], username: Option[String]) extends Conversation

object User {
  implicit val userFormat: Format[User] = Json.format[User]
}