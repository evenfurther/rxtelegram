package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Format}

case class ReplyKeyboardHide(hide_keyboard: Boolean = true, selective: Option[Boolean] = None) extends ReplyMarkup

object ReplyKeyboardHide {
  implicit val replyKeyboardHideFormat: Format[ReplyKeyboardHide] = Json.format[ReplyKeyboardHide]
}
