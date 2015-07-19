package net.rfc1149.rxtelegram.model.media

import akka.http.scaladsl.model.{ContentType, HttpEntity, MediaType}
import akka.http.scaladsl.model.Multipart.FormData.BodyPart

case class MediaData(mediaType: MediaType, data: Array[Byte], fileName: Option[String] = None) extends Media {
  def toBodyPart(fieldName: String) = BodyPart(fieldName, HttpEntity(ContentType(mediaType), data),
    Map("filename" -> fileName.getOrElse(s"media.${mediaType.subType}")))
}
