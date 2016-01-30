package net.rfc1149.rxtelegram.model.media

import java.io.File

import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model.{ContentType, MediaType}

case class MediaFile(mediaType: MediaType.WithFixedCharset, file: File) extends Media {
  def toBodyPart(fieldName: String) = BodyPart.fromFile(fieldName, ContentType(mediaType), file)
}
