package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Format}

case class Video(file_id: String, width: Int, height: Int, duration: Int, thumb: PhotoSize,
                mime_type: Option[String], file_size: Option[Int], caption: Option[String])

object Video {
  implicit val videoFormat: Format[Video] = Json.format[Video]
}
