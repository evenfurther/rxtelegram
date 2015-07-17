package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Format}

case class Sticker(file_id: String, width: Int, height: Int, thumb: PhotoSize, file_size: Option[Int])

object Sticker {
  implicit val stickerFormat: Format[Sticker] = Json.format[Sticker]
}
