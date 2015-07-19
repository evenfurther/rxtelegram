package net.rfc1149.rxtelegram.model

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

case class Message(message_id: Long, from: User, date: Long, chat: Conversation,
                  forward_from: Option[User], forward_date: Option[Long],
                  reply_to_message: Option[Message], text: Option[String],
                  audio: Option[Audio], voice: Option[Voice], document: Option[Document],
                  photo: Option[Array[PhotoSize]], sticker: Option[Sticker],
                  video: Option[Video], caption: Option[String],
                  contact: Option[Contact], location: Option[Location],
                  new_chat_participant: Option[User], left_chat_participant: Option[User],
                  new_chat_title: Option[String], new_chat_photo: Option[Array[PhotoSize]],
                  delete_chat_photo: Option[Boolean], group_chat_created: Option[Boolean])

object Message {

  // The reads predefined methods work up to 22 fields, and we have more than that. This is why
  // we need to have this ugly deconstruction of the JSON object.

  private lazy val messageReadA: Reads[(Long, User, Long, Conversation, Option[User], Option[Long])] =
    ((JsPath \ "message_id").read[Long] and
    (JsPath \ "from").read[User] and
    (JsPath \ "date").read[Long] and
    (JsPath \ "chat").read[Conversation] and
    (JsPath \ "forward_from").readNullable[User] and
    (JsPath \ "forward_date").readNullable[Long]).tupled
  private lazy val messageReadB: Reads[(Option[Message], Option[String], Option[Audio], Option[Voice], Option[Document], Option[Array[PhotoSize]])] =
    ((JsPath \ "reply_to_message").lazyReadNullable(messageReads) and
    (JsPath \ "text").readNullable[String] and
    (JsPath \ "audio").readNullable[Audio] and
    (JsPath \ "voice").readNullable[Voice] and
    (JsPath \ "document").readNullable[Document] and
    (JsPath \ "photo").readNullable[Array[PhotoSize]]).tupled
  private lazy val messageReadC: Reads[(Option[Sticker], Option[Video], Option[String], Option[Contact], Option[Location])] =
    ((JsPath \ "sticker").readNullable[Sticker] and
    (JsPath \ "video").readNullable[Video] and
    (JsPath \ "caption").readNullable[String] and
    (JsPath \ "contact").readNullable[Contact] and
    (JsPath \ "location").readNullable[Location]).tupled
  private lazy val messageReadD: Reads[(Option[User], Option[User], Option[String], Option[Array[PhotoSize]], Option[Boolean], Option[Boolean])] =
    ((JsPath \ "new_chat_participant").readNullable[User] and
      (JsPath \ "left_chat_participant").readNullable[User] and
      (JsPath \ "new_chat_title").readNullable[String] and
      (JsPath \ "new_chat_photo").readNullable[Array[PhotoSize]] and
      (JsPath \ "delete_chat_photo").readNullable[Boolean] and
      (JsPath \ "group_chat_created").readNullable[Boolean]).tupled

  private def createFromParts(a:(Long, User, Long, Conversation, Option[User], Option[Long]),
            b: (Option[Message], Option[String], Option[Audio], Option[Voice], Option[Document], Option[Array[PhotoSize]]),
            c: (Option[Sticker], Option[Video], Option[String], Option[Contact], Option[Location]),
            d: (Option[User], Option[User], Option[String], Option[Array[PhotoSize]], Option[Boolean], Option[Boolean])): Message = {
    val (message_id, from, date, chat, forward_from, forward_date) = a
    val (reply_to_message, text, audio, voice, document, photo) = b
    val (sticker, video, caption, contact, location) = c
    val (new_chat_participant, left_chat_participant, new_chat_title, new_chat_photo, delete_chat_photo, group_chat_created) = d
    Message(message_id, from, date, chat, forward_from, forward_date,
      reply_to_message, text, audio, voice, document, photo,
      sticker, video, caption, contact, location,
      new_chat_participant, left_chat_participant, new_chat_title, new_chat_photo, delete_chat_photo, group_chat_created)
  }

  implicit lazy val messageReads: Reads[Message] =
    (messageReadA and messageReadB and messageReadC and messageReadD)(createFromParts _)
}
