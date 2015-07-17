package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Format, Writes, Reads}

trait Conversation {
  val id: Int
}

object Conversation {
  implicit val conversationReads: Reads[Conversation] =
    User.userFormat.asInstanceOf[Reads[Conversation]] orElse GroupChat.groupChatFormat.asInstanceOf[Reads[Conversation]]
  implicit val conversationWrites: Writes[Conversation] = Writes {
    case (user: User)           => User.userFormat.writes(user)
    case (groupChat: GroupChat) => GroupChat.groupChatFormat.writes(groupChat)
  }
  implicit val conversationFormat: Format[Conversation] = Format(conversationReads, conversationWrites)
}