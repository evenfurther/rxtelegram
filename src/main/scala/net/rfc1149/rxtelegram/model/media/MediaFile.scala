package net.rfc1149.rxtelegram.model.media

import java.io.File

import akka.http.scaladsl.model.{ContentType, MediaType}
import akka.http.scaladsl.model.Multipart.FormData.BodyPart

case class MediaFile(mediaType: MediaType, file: File) extends Media {
  def toBodyPart(fieldName: String) = BodyPart.fromFile(fieldName, ContentType(mediaType), file)
}
