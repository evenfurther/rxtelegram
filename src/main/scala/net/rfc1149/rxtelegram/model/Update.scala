package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Reads}

case class Update(update_id: Long, message: Option[Message], inline_query: Option[InlineQuery],
  chosen_inline_result: Option[ChosenInlineResult])

object Update {
  implicit val updateReads: Reads[Update] = Json.reads[Update]
}
