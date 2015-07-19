package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Format}

case class Voice(file_id: String, duration: Long, mime_type: Option[String], file_size: Option[Long])

object Voice {
  implicit val voiceFormat: Format[Voice] = Json.format[Voice]
}
