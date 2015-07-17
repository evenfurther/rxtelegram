package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Reads, Writes}

trait ReplyMarkup

object ReplyMarkup {
  implicit val replyMarkupWrites: Writes[ReplyMarkup] = Writes {
    case fr: ForceReply => ForceReply.forceReplyFormat.writes(fr)
    case rkh: ReplyKeyboardHide => ReplyKeyboardHide.replyKeyboardHideFormat.writes(rkh)
    case rkm: ReplyKeyboardMarkup => ReplyKeyboardMarkup.replyKeyboardMarkupFormat.writes(rkm)
  }
}
