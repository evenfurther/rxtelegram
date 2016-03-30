package net.rfc1149.rxtelegram.model

import net.rfc1149.rxtelegram.Bot.ParseMode
import play.api.libs.json.{Json, Writes}

case class InlineQueryResultPhoto(id: String, photo_url: String, photo_width: Option[Long] = None, photo_height: Option[Long] = None,
  thumb_url: Option[String] = None, title: Option[String] = None,
  description: Option[String] = None, caption: Option[String] = None,
  message_text: Option[String] = None, parse_mode: Option[ParseMode] = None,
  disable_web_page_preview: Option[Boolean] = None)

object InlineQueryResultPhoto {
  implicit val inlineQueryResultPhotoWrites: Writes[InlineQueryResultPhoto] = Writes { iqrp ⇒
    Json.writes[InlineQueryResultPhoto].writes(iqrp) ++ Json.obj("type" → "photo")
  }
}
