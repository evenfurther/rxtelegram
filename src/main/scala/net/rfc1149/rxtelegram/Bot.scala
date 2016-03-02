package net.rfc1149.rxtelegram

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, PredefinedFromEntityUnmarshallers, Unmarshal}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import net.rfc1149.rxtelegram.model._
import net.rfc1149.rxtelegram.model.media.Media
import net.rfc1149.rxtelegram.utils._
import play.api.libs.json._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait Bot {

  implicit val actorSystem: ActorSystem
  implicit val ec: ExecutionContext
  implicit val fm: Materializer

  import Bot._

  val token: String

  private[this] var offset: Long = -1

  private[this] def send(methodName: String, fields: Seq[(String, String)] = Seq(), media: Option[MediaParameter] = None,
                         potentiallyBlocking: Boolean = false): Future[JsValue] =
    sendInternal(methodName, buildEntity(fields, media), potentiallyBlocking = potentiallyBlocking)

  def sendToMessage(data: Command): Future[Message] = send(data).map(_.as[Message])

  def send(data: Command): Future[JsValue] = {
    sendInternal(data.methodName, data.buildEntity(includeMethod = false)).map(checkResult)
  }

  private[this] lazy val host = "api.telegram.org"
  private[this] lazy val port = 443

  // Marking those private leads to an instantiation bug in Scala 2.11.7
  private[this] lazy val apiPool = Http().newHostConnectionPoolHttps[Any](host, port)
  private[this] lazy val apiFlow = Http().outgoingConnectionHttps(host, port)

  private[this] def sendRaw(request: HttpRequest, potentiallyBlocking: Boolean = false): Future[HttpResponse] =
    if (potentiallyBlocking)
      Source.single(request).via(apiFlow).runWith(Sink.head)
    else
      Source.single((request, None)).via(apiPool).map(_._1.get).runWith(Sink.head)

  private[this] def sendInternal(methodName: String, entity: MessageEntity, potentiallyBlocking: Boolean = false): Future[JsValue] = {
    val request = HttpRequest(method = HttpMethods.POST,
      uri = s"https://api.telegram.org/bot$token/$methodName",
      headers = List(`Accept`(MediaTypes.`application/json`)),
      entity = entity)
    sendRaw(request, potentiallyBlocking = potentiallyBlocking).flatMap { response =>
      response.status match {
        case status if status.isFailure() => throw HTTPException(status.toString())
        case status =>
          try {
            Unmarshal(response.entity).to[JsValue]
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
    send("getUpdates", (offset + 1).toField("offset") ++  limit.toField("limit", 100) ++ timeout.toSeconds.toField("timeout", 0),
      potentiallyBlocking = true)
      .map { json => (json \ "result").as[List[Update]] }

  def getUserProfilePhotos(user_id: Long, offset: Long = 0, limit: Long = 100): Future[UserProfilePhotos] =
    send("getUserProfilePhotos", user_id.toField("user_id") ++ offset.toField("offset", 0) ++ limit.toField("limit", 100))
      .map { json => (json \ "result").as[UserProfilePhotos] }

  def getFile(file_id: String): Future[(File, Option[ResponseEntity])] = {
    send("getFile", file_id.toField("file_id")).map { json => (json \ "result").as[File] }.flatMap { file =>
      file.file_path match {
        case Some(path) =>
          sendRaw(HttpRequest(method = HttpMethods.GET, uri = s"https://api.telegram.org/file/bot$token/$path",
            headers = List(`Accept`(MediaRanges.`*/*`)))).map(response => (file, Some(response.entity)))
        case None =>
          Future.successful((file, None))
      }
    }
  }

  protected[this] def acknowledgeUpdate(update: Update): Unit =
    offset = offset.max(update.update_id)

  def setWebhook(uri: String, certificate: Option[Media] = None): Future[JsValue] =
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

  def checkResult(js: JsValue): JsValue =
    if ((js \ "ok").as[Boolean])
      (js \ "result").as[JsValue]
    else
      throw APIException((js \ "description").as[String])

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
    val chat_id: String
    val message_id: Option[Long] = None
    def toFields: List[(String, String)] = chat_id.toField("chat_id") ++ message_id.toField("in_reply_to_message_id")
  }

  case class To(chat_id: String) extends Target

  object To {
    def apply(chat_id: Long): To = To(chat_id.toString)
    def apply(message: Message): To = To(message.chat.id)
    def apply(chat: Chat): To = To(chat.id)
    def apply(user: User): To = To(user.id)
  }

  case class Reply(chat_id: String, mid: Long) extends Target {
    override val message_id = Some(mid)
  }

  object Reply {
    def apply(message: Message): Reply = Reply(message.chat.id.toString, message.message_id)
  }

  sealed trait Command {
    def buildEntity(includeMethod: Boolean): MessageEntity
    val methodName: String
  }

  sealed trait Action extends Command {
    val methodName: String
    val replyMarkup: Option[ReplyMarkup]

    val fields: List[(String, String)]
    val media: Option[MediaParameter] = None

    def buildEntity(target: Target, includeMethod: Boolean) = {
      val allFields = fields ++ replyMarkup.toField("reply_markup") ++ target.toFields ++ (if (includeMethod) Seq("method" -> methodName) else Seq())
      Bot.buildEntity(allFields, media)
    }

    override def buildEntity(includeMethod: Boolean) = {
      val allFields = fields ++ replyMarkup.toField("reply_markup") ++ (if (includeMethod) Seq("method" -> methodName) else Seq())
      Bot.buildEntity(allFields, media)
    }

    protected def namedMedia(name: String, media: Media) = Some(MediaParameter(name, media))
  }

  case class Targetted(target: Target, action: Action) extends Command {
    override def buildEntity(includeMethod: Boolean) =
      action.buildEntity(target, includeMethod)

    val methodName = action.methodName
  }

  case class ActionForwardMessage(message: Reply) extends Action {
    val methodName = "forwardMessage"
    val replyMarkup = None
    val fields = message.chat_id.toField("from_chat_id") ++ message.message_id.toField("message_id")
  }

  object ActionForwardMessage {
    def apply(message: Message): ActionForwardMessage = ActionForwardMessage(Reply(message))
  }

  case class ActionMessage(text: String, disable_web_page_preview: Boolean = false, disable_notification: Boolean = false,
                           parse_mode: ParseMode = ParseModeDefault, replyMarkup: Option[ReplyMarkup] = None) extends Action {
    val methodName = "sendMessage"
    val fields = text.toField("text") ++ disable_web_page_preview.toField("disable_web_page_preview", false) ++
      disable_notification.toField("disable_notification", false) ++
      parse_mode.option.toField("parse_mode")
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

  case class ActionAnswerInlineQuery(inlineQueryId: String, results: Seq[InlineQueryResult],
    cacheTime: Long = 300, isPersonal: Boolean = false, nextOffset: Option[String] = None) extends Action {
    val methodName = "answerInlineQuery"
    val replyMarkup = None
    val fields = inlineQueryId.toField("inline_query_id") ++
      Json.stringify(Json.toJson(results)).toField("results") ++
      cacheTime.toField("cache_time") ++ isPersonal.toField("is_personal", false) ++ nextOffset.toField("next_offset")
  }

  sealed trait ParseMode {
    val option: Option[String]
  }

  object ParseMode {
    implicit val parseModeWrites: Writes[ParseMode] = Writes { pm => Json.toJson(pm.option) }
  }

  object ParseModeDefault extends ParseMode {
    val option = None
  }

  object ParseModeMarkdown extends ParseMode {
    val option = Some("Markdown")
  }

  object ParseModeHTML extends ParseMode {
    val option = Some("HTML")
  }

  def buildEntity(fields: Seq[(String, String)], media: Option[MediaParameter]): MessageEntity = {
    if (media.isDefined) {
      val data = fields.map { case (k, v) => BodyPart(k, HttpEntity(v)) }
      Multipart.FormData(media.get.toBodyPart :: data.toList: _*).toEntity()
    } else if (fields.isEmpty)
      HttpEntity.Empty
    else
      FormData(fields.toMap).toEntity
  }

}

