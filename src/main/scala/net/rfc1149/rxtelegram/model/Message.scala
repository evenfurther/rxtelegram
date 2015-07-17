package net.rfc1149.rxtelegram.model

import play.api.libs.json.{Format, Json}

case class Message(message_id: Int, from: User, date: Int, chat: Conversation,
                  forward_from: Option[User], forward_date: Option[Int],
                  reply_to_message: Option[Message], text: Option[String],
                  audio: Option[Audio], document: Option[Document],
                  photo: Option[Array[PhotoSize]], sticker: Option[Sticker],
                  video: Option[Video], contact: Option[Contact], location: Option[Location],
                  new_chat_participant: Option[User], left_chat_participant: Option[User],
                  new_chat_title: Option[String], new_chat_photo: Option[Array[PhotoSize]],
                  delete_chat_photo: Option[Boolean], group_chat_created: Option[Boolean])

object Message {
  implicit val messageFormat: Format[Message] = Json.format[Message]
}
