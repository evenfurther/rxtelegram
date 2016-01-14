package net.rfc1149.rxtelegram.model

import net.rfc1149.rxtelegram.Bot.ParseMode
import play.api.libs.json.{Json, Writes}

case class InlineQueryResultMpeg4Gif(id: String, mpeg4_url: String, mpeg4_width: Option[Long] = None, mpeg4_height: Option[Long] = None,
                                     thumb_url: String, title: Option[String] = None, caption: Option[String] = None,
                                     message_text: Option[String] = None, parse_mode: Option[ParseMode] = None,
                                     disable_web_page_preview: Option[Boolean] = None)

object InlineQueryResultMpeg4Gif {
  implicit val inlineQueryResultMpeg4GifWrites: Writes[InlineQueryResultMpeg4Gif] = Writes { iqrm =>
    Json.writes[InlineQueryResultMpeg4Gif].writes(iqrm) ++ Json.obj("type" -> "mpeg4_gif")
  }
}
