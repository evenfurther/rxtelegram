package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Reads, Json, Format}

case class User(id: Long, first_name: String, last_name: Option[String], username: Option[String]) extends Equals {

  override def canEqual(other: Any): Boolean = other.isInstanceOf[User]

  override def equals(other: Any): Boolean = id == other.asInstanceOf[User].id
}

object User {
  implicit val userFormat: Format[User] = Json.format[User]
}