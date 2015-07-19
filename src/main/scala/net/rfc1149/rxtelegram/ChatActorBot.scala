package net.rfc1149.rxtelegram

import akka.actor.{Actor, ActorLogging}
import net.rfc1149.rxtelegram.ActorBot._
import net.rfc1149.rxtelegram.Bot._
import net.rfc1149.rxtelegram.model._
import net.rfc1149.rxtelegram.model.media.Media

import scala.concurrent.duration.FiniteDuration

trait ChatActorBot extends Actor with ActorLogging {

  import ChatActorBot._

  protected[this] var me: User = _
  protected[this] var chat: Conversation = null
  protected[this] var target: To = _

  def ready_to_send(): Unit = {}
  def ready(): Unit = {}

  def handleMessage(message: Message): Unit

  def handleOther(other: Any): Unit =
    context.parent.forward(other)

  def receive = {
    case (user: User, chat_id: Long) =>
      me = user
      target = To(chat_id)
      try {
        ready_to_send()
      } catch {
        case t: Throwable =>
          log.error(t, "receiving chat information")
      }
    case message: Message =>
      if (chat == null) {
        chat = message.chat
        try {
          ready()
        } catch {
          case t: Throwable =>
            log.error(t, "receiving initial message from peer")
        }
      }
      try {
        handleMessage(message)
      } catch {
        case t: Throwable =>
          log.error(t, s"handling message $message")
      }
    case TargetMessage(text, disable_web_page_preview, replyMarkup) =>
      context.parent.forward(SendMessage(target, text, disable_web_page_preview, replyMarkup))
    case TargetPhoto(photo, caption, replyMarkup) =>
      context.parent.forward(SendPhoto(target, photo, caption, replyMarkup))
    case TargetAudio(audio, duration, performer, title, replyMarkup) =>
      context.parent.forward(SendAudio(target, audio, duration, performer, title, replyMarkup))
    case TargetVoice(voice, duration, replyMarkup) =>
      context.parent.forward(SendVoice(target, voice, duration, replyMarkup))
    case TargetDocument(document, replyMarkup) =>
      context.parent.forward(SendDocument(target, document, replyMarkup))
    case TargetSticker(sticker, replyMarkup) =>
      context.parent.forward(SendSticker(target, sticker, replyMarkup))
    case TargetVideo(video, duration, caption, replyMarkup) =>
      context.parent.forward(SendVideo(target, video, duration, caption, replyMarkup))
    case TargetLocation(location, replyMarkup) =>
      context.parent.forward(SendLocation(target, location, replyMarkup))
    case TargetChatAction(action) =>
      context.parent.forward(SendChatAction(target, action))
    case TargetForwardMessage(origin) =>
      context.parent.forward(ForwardMessage(target, origin))
    case other =>
      try {
        handleOther(other)
      } catch {
        case t: Throwable =>
          log.error(t, s"handling other data $other")
      }
  }

}

object ChatActorBot {

  sealed trait TargetCommunication

  case class TargetMessage(text: String, disable_web_page_preview: Boolean = false, replyMarkup: Option[ReplyMarkup] = None) extends TargetCommunication

  case class TargetPhoto(photo: Media, caption: Option[String] = None, replyMarkup: Option[ReplyMarkup] = None) extends TargetCommunication

  case class TargetAudio(audio: Media, duration: Option[FiniteDuration] = None,
                         performer: Option[String] = None, title: Option[String] = None,
                         replyMarkup: Option[ReplyMarkup] = None) extends TargetCommunication

  case class TargetVoice(voice: Media, duration: Option[FiniteDuration] = None, replyMarkup: Option[ReplyMarkup] = None) extends TargetCommunication

  case class TargetDocument(document: Media, replyMarkup: Option[ReplyMarkup] = None) extends TargetCommunication

  case class TargetSticker(sticker: Media, replyMarkup: Option[ReplyMarkup] = None) extends TargetCommunication

  case class TargetVideo(video: Media, duration: Option[FiniteDuration] = None,
                         caption: Option[String] = None, replyMarkup: Option[ReplyMarkup] = None) extends TargetCommunication

  case class TargetLocation(location: (Double, Double), replyMarkup: Option[ReplyMarkup] = None) extends TargetCommunication
  object TargetLocation {
    def apply(location: Location): TargetLocation = TargetLocation((location.latitude, location.longitude))
  }

  case class TargetChatAction(action: ChatAction) extends TargetCommunication

  case class TargetForwardMessage(origin: Message) extends TargetCommunication
}
