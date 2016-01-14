package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Reads}

case class InlineQuery(id: String, from: User, query: String, offset: String)

object InlineQuery {
  implicit val inlineQueryReads: Reads[InlineQuery] = Json.reads[InlineQuery]
}