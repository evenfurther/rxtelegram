package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Format}

case class ForceReply(force_reply: Boolean = true, selective: Option[Boolean] = None) extends ReplyMarkup

object ForceReply {
  implicit val forceReplyFormat: Format[ForceReply] = Json.format[ForceReply]
}
