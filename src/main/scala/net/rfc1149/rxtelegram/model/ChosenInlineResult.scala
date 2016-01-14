package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Reads}

case class ChosenInlineResult(result_id: String, from: User, query: String)

object ChosenInlineResult {
  implicit val chosenInlineResultReads: Reads[ChosenInlineResult] = Json.reads[ChosenInlineResult]
}
