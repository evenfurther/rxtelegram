package net.rfc1149.rxtelegram.model

import net.rfc1149.rxtelegram.Bot.ParseMode
import play.api.libs.json.{Json, Writes}

case class InlineQueryResultGif(id: String, gif_url: String, gif_width: Option[Long] = None, gif_height: Option[Long] = None,
  thumb_url: String, title: Option[String] = None, caption: Option[String] = None,
  message_text: Option[String] = None, parse_mode: Option[ParseMode] = None,
  disable_web_page_preview: Option[Boolean] = None)

object InlineQueryResultGif {
  implicit val inlineQueryResultGifWrites: Writes[InlineQueryResultGif] = Writes { iqrg ⇒
    Json.writes[InlineQueryResultGif].writes(iqrg) ++ Json.obj("type" → "gif")
  }
}
