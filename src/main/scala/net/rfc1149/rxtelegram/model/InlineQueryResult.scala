package net.rfc1149.rxtelegram.model

import play.api.libs.json.Writes

trait InlineQueryResult

object InlineQueryResult {
  implicit val inlineQueryResultWrites: Writes[InlineQueryResult] = Writes {
    case iqra: InlineQueryResultArticle => InlineQueryResultArticle.inlineQueryResultArticleWrites.writes(iqra)
    case iqrg: InlineQueryResultGif => InlineQueryResultGif.inlineQueryResultGifWrites.writes(iqrg)
    case iqrm: InlineQueryResultMpeg4Gif => InlineQueryResultMpeg4Gif.inlineQueryResultMpeg4GifWrites.writes(iqrm)
    case iqrp: InlineQueryResultPhoto => InlineQueryResultPhoto.inlineQueryResultPhotoWrites.writes(iqrp)
    case iqrv: InlineQueryResultVideo => InlineQueryResultVideo.inlineQueryResultVideoWrites.writes(iqrv)
  }
}
