package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Format}

case class Document(file_id: String, thumb: Option[PhotoSize], file_name: Option[String], mime_type: Option[String], file_size: Option[Long])

object Document {
  implicit val documentFormat: Format[Document] = Json.format[Document]
}
