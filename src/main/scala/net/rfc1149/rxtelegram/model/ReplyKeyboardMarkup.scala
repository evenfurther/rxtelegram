package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Format, Json}

case class ReplyKeyboardMarkup(keyboard: Array[Array[String]], resize_keyboard: Option[Boolean] = None,
  one_time_keyboard: Option[Boolean] = None, selective: Option[Boolean] = None) extends ReplyMarkup

object ReplyKeyboardMarkup {
  implicit val replyKeyboardMarkupFormat: Format[ReplyKeyboardMarkup] = Json.format[ReplyKeyboardMarkup]
}