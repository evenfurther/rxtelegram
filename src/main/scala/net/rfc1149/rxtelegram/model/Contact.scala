package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Format}

case class Contact(phone_number: String, first_name: String, last_name: Option[String], user_id: Option[Int])

object Contact {
  implicit val contactFormat: Format[Contact] = Json.format[Contact]
}
