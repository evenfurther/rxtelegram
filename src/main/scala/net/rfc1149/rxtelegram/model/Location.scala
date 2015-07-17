package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Json, Format}

case class Location(longitude: Float, latitude: Float)

object Location {
  implicit val locationFormat: Format[Location] = Json.format[Location]
}
