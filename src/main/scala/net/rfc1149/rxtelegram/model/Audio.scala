package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Format}

case class Audio(file_id: String, duration: Int, mime_type: Option[String], file_size: Option[Int])

object Audio {
  implicit val audioFormat: Format[Audio] = Json.format[Audio]
}
