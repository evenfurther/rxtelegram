package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Format}

case class UserProfilePhotos(total_count: Int, photos: Array[Array[PhotoSize]])

object UserProfilePhotos {
  implicit val userProfilePhotosFormat: Format[UserProfilePhotos] = Json.format[UserProfilePhotos]
}
