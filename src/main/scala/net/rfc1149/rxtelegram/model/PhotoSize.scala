package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Format}

case class PhotoSize(file_id: String, width: Int, height: Int, file_size: Option[Int])

object PhotoSize {
  implicit val photoSizeFormat: Format[PhotoSize] = Json.format[PhotoSize]
}
