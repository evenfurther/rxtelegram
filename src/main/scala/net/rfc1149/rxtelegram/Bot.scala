package net.rfc1149.rxtelegram

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.HostConnectionPool
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, PredefinedFromEntityUnmarshallers, Unmarshal}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Source}
import net.rfc1149.rxtelegram.model._
import net.rfc1149.rxtelegram.model.media.Media
import net.rfc1149.rxtelegram.utils._
import play.api.libs.json.{JsObject, Json, Reads}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait Bot {

  implicit val actorSystem: ActorSystem
  implicit val ec: ExecutionContext
  implicit val fm: Materializer

  import Bot._

  val token: String

  private[this] var offset: Long = -1

  private[this] def send(methodName: String, fields: Seq[(String, String)] = Seq(), media: Option[MediaParameter] = None): Future[JsObject] =
    sendInternal(methodName, buildEntity(fields, media))

  def send(data: Send): Future[Message] = sendInternal(data.action.methodName, data.buildEntity(includeMethod = false)).toMessage

  private[this] def sendInternal(methodName: String, entity: Future[MessageEntity]): Future[JsObject] = {
    entity.map { fd =>
      HttpRequest()
        .withUri(s"https://api.telegram.org/bot$token/$methodName")
        .withHeaders(List(`Accept`(MediaTypes.`application/json`)))
        .withMethod(HttpMethods.POST)
        .withEntity(fd)
    }.flatMap { request =>
      val freshPool: Flow[(HttpRequest, Any), (Try[HttpResponse], Any), HostConnectionPool] = Http().newHostConnectionPoolTls("api.telegram.org", 443)
      Source.single((request, None)).via(freshPool).runFold[HttpResponse](null) {
        case (_, (r, _)) => r.get
      }
    }.flatMap { response =>
      response.status match {
        case status if status.isFailure() => throw HTTPException(status.toString())
        case status =>
          try {
            Unmarshal(response.entity).to[JsObject]
          } catch {
            case t: Throwable =>
              throw JSONException(t)
          }
      }
    }
  }

  def getMe: Future[User] =
    send("getMe").map { json =>
      (json \ "result").as[User]
    }

  def getUpdates(limit: Long = 100, timeout: FiniteDuration = 0.seconds): Future[List[Update]] =
    send("getUpdates", (offset + 1).toField("offset") ++  limit.toField("limit", 100) ++ timeout.toSeconds.toField("timeout", 0))
      .map { json => (json \ "result").as[List[Update]] }

  def getUserProfilePhotos(user_id: Long, offset: Long = 0, limit: Long = 100): Future[UserProfilePhotos] =
    send("getUserProfilePhotos", user_id.toField("user_id") ++ offset.toField("offset", 0) ++ limit.toField("limit", 100))
      .map { json => (json \ "result").as[UserProfilePhotos] }

  protected[this] def acknowledgeUpdate(update: Update): Unit =
    offset = offset.max(update.update_id)

  def setWebhook(uri: String, certificate: Option[Media] = None): Future[JsObject] =
    send("setWebhook", Seq("url" -> uri), certificate.map(MediaParameter("certificate", _)))

}

object Bot {

  implicit def jsonUnmarshaller[T: Reads](implicit fm: Materializer, ec: ExecutionContext): FromEntityUnmarshaller[T] =
    PredefinedFromEntityUnmarshallers.stringUnmarshaller.forContentTypes(`application/json`)
      .map(s => implicitly[Reads[T]].reads(Json.parse(s)).recoverTotal(e => throw new RuntimeException(s"Exception $e when parsing $s")))

  sealed trait ChatAction {
    val action: String
  }
  case object Typing extends ChatAction {
    val action = "typing"
  }
  case object UploadPhoto extends ChatAction {
    val action = "upload_photo"
  }
  case object RecordVideo extends ChatAction {
    val action = "record_video"
  }
  case object UploadVideo extends ChatAction {
    val action = "upload_video"
  }
  case object RecordAudio extends ChatAction {
    val action = "record_audio"
  }
  case object UploadAudio extends ChatAction {
    val action = "upload_audio"
  }
  case object UploadDocument extends ChatAction {
    val action = "upload_document"
  }
  case object FindLocation extends ChatAction {
    val action = "find_location"
  }

  case class MediaParameter(fieldName: String, media: Media) {
    def toBodyPart = media.toBodyPart(fieldName)
  }

  private[Bot] implicit class ToMessage(json: Future[JsObject]) {
    def toMessage(implicit ec: ExecutionContext): Future[Message] = json.map { js =>
      if ((js \ "ok").as[Boolean])
        (js \ "result").as[Message]
      else
        throw APIException((js \ "description").as[String])
    }
  }

  sealed trait TelegramException extends Exception

  case class HTTPException(status: String) extends TelegramException {
    override val toString = s"HTTPException($status)"
  }
  case class APIException(description: String) extends TelegramException {
    override val toString = s"APIException($description)"
  }
  case class JSONException(inner: Throwable) extends TelegramException {
    override val toString = s"JSONException($inner)"
  }

  sealed trait Target {
    val chat_id: Long
    val message_id: Option[Long] = None
    def toFields: List[(String, String)] = chat_id.toField("chat_id") ++ message_id.toField("in_reply_to_message_id")
  }

  case class To(chat_id: Long) extends Target

  object To {
    def apply(message: Message): To = To(message.chat.id)
    def apply(user: User): To = To(user.id)
  }

  case class Reply(chat_id: Long, mid: Long) extends Target {
    override val message_id = Some(mid)
  }

  object Reply {
    def apply(message: Message): Reply = Reply(message.chat.id, message.message_id)
  }

  sealed trait Action {
    val methodName: String
    val replyMarkup: Option[ReplyMarkup]

    val fields: List[(String, String)]
    val media: Option[MediaParameter] = None

    def buildEntity(target: Target, includeMethod: Boolean)(implicit ec: ExecutionContext) = {
      val allFields = fields ++ replyMarkup.toField("reply_markup") ++ target.toFields ++ (if (includeMethod) Seq("method" -> methodName) else Seq())
      Bot.buildEntity(allFields, media)
    }

    protected def namedMedia(name: String, media: Media) = Some(MediaParameter(name, media))
  }

  case class Send(target: Target, action: Action) {
    def buildEntity(includeMethod: Boolean)(implicit ec: ExecutionContext) =
      action.buildEntity(target, includeMethod)
  }

  case class ActionForwardMessage(message: Reply) extends Action {
    val methodName = "forwardMessage"
    val replyMarkup = None
    val fields = message.chat_id.toField("from_chat_id") ++ message.message_id.toField("message_id")
  }

  object ActionForwardMessage {
    def apply(message: Message): ActionForwardMessage = ActionForwardMessage(Reply(message))
  }

  case class ActionMessage(text: String, disable_web_page_preview: Boolean = false,
                           replyMarkup: Option[ReplyMarkup] = None) extends Action {
    val methodName = "sendMessage"
    val fields = text.toField("text") ++ disable_web_page_preview.toField("disable_web_page_preview", false)
  }

  case class ActionPhoto(photo: Media, caption: Option[String] = None, replyMarkup: Option[ReplyMarkup] = None) extends Action {
    val methodName = "sendPhoto"
    val fields = caption.toField("caption")
    override val media = namedMedia("photo", photo)
  }

  case class ActionAudio(audio: Media, duration: Option[FiniteDuration] = None,
                         performer: Option[String], title: Option[String], replyMarkup: Option[ReplyMarkup] = None) extends Action {
    val methodName = "sendAudio"
    val fields = duration.toField("duration") ++ performer.toField("performer") ++ title.toField("title")
    override val media = namedMedia("audio", audio)
  }

  case class ActionVoice(voice: Media, duration: Option[FiniteDuration] = None, replyMarkup: Option[ReplyMarkup] = None) extends Action {
    val methodName = "sendVoice"
    val fields = duration.toField("duration")
    override val media = namedMedia("voice", voice)
  }

  case class ActionDocument(document: Media, replyMarkup: Option[ReplyMarkup] = None) extends Action {
    val methodName = "sendDocument"
    val fields = Nil
    override val media = namedMedia("document", document)
  }

  case class ActionSticker(sticker: Media, replyMarkup: Option[ReplyMarkup] = None) extends Action {
    val methodName = "sendSticker"
    val fields = Nil
    override val media = namedMedia("sticker", sticker)
  }

  case class ActionVideo(video: Media, duration: Option[FiniteDuration] = None,
                         caption: Option[String], replyMarkup: Option[ReplyMarkup] = None) extends Action {
    val methodName = "sendVideo"
    val fields = duration.map(_.toSeconds).toField("duration") ++ caption.toField("caption")
    override val media = namedMedia("video", video)
  }

  case class ActionLocation(location: (Double, Double), replyMarkup: Option[ReplyMarkup] = None) extends Action {
    val methodName = "sendLocation"
    val fields = location._1.toField("latitude") ++ location._2.toField("longitude")
  }

  object ActionLocation {
    def apply(location: Location): ActionLocation = ActionLocation((location.latitude, location.longitude))
  }

  case class ActionChatAction(action: ChatAction) extends Action {
    val methodName = "sendChatAction"
    val replyMarkup = None
    val fields = action.action.toField("action")
  }

  def buildEntity(fields: Seq[(String, String)], media: Option[MediaParameter])(implicit ec: ExecutionContext): Future[MessageEntity] = {
    if (media.isDefined) {
      val data = fields.map { case (k, v) => BodyPart(k, HttpEntity(v)) }
      Marshal(Multipart.FormData(media.get.toBodyPart :: data.toList: _*)).to[MessageEntity]
    } else if (fields.isEmpty)
      Future.successful(HttpEntity.Empty)
    else
      Marshal(FormData(fields.toMap)).to[MessageEntity]
  }

}

