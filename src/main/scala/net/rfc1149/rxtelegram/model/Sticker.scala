package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Format}

case class Sticker(file_id: String, width: Long, height: Long, thumb: Option[PhotoSize], file_size: Option[Long])

object Sticker {
  implicit val stickerFormat: Format[Sticker] = Json.format[Sticker]
}
