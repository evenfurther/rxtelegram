package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Format}

case class ReplyKeyboardMarkup(keyboard: Array[Array[String]], resize_keyboard: Option[Boolean],
  one_time_keyboard: Option[Boolean], selective: Option[Boolean]) extends ReplyMarkup

object ReplyKeyboardMarkup {
  implicit val replyKeyboardMarkupFormat: Format[ReplyKeyboardMarkup] = Json.format[ReplyKeyboardMarkup]
}