package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Reads, Json, Format}

case class GroupChat (id: Long, title: String) extends Conversation

object GroupChat {
  implicit val groupChatFormat: Format[GroupChat] = Json.format[GroupChat]
}
