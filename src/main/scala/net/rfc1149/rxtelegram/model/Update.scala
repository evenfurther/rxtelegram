package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Format}

case class Update(update_id: Int, message: Option[Message])

object Update {
  implicit val updateFormat: Format[Update] = Json.format[Update]
}
