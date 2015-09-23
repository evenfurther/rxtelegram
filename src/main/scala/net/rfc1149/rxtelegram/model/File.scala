package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Format}

case class File(file_id: String, file_size: Option[Long], file_path: Option[String])

object File {
  implicit val fileFormats: Format[File] = Json.format[File]
}
