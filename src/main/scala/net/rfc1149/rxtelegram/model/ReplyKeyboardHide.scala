package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Format, Json}

case class ReplyKeyboardHide(hide_keyboard: Boolean = true, selective: Option[Boolean] = None) extends ReplyMarkup

object ReplyKeyboardHide {
  implicit val replyKeyboardHideFormat: Format[ReplyKeyboardHide] = Json.format[ReplyKeyboardHide]
}
