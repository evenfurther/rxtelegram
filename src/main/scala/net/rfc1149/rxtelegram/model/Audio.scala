package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Format}

case class Audio(file_id: String, duration: Long, performer: Option[String], title: Option[String],
                 mime_type: Option[String], file_size: Option[Long])

object Audio {
  implicit val audioFormat: Format[Audio] = Json.format[Audio]
}
