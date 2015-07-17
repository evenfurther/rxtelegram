package net.rfc1149.rxtelegram

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, PredefinedFromEntityUnmarshallers}
import akka.stream.{ActorMaterializer, Materializer}
import net.rfc1149.rxtelegram.model.{Message, ReplyMarkup, Update, User}
import play.api.libs.json.{JsObject, Json, Reads}

import scala.concurrent.{ExecutionContext, Future}

trait Bot {

  implicit val actorSystem: ActorSystem
  implicit lazy val ec: ExecutionContext = actorSystem.dispatcher
  implicit lazy val fm: Materializer = ActorMaterializer()

  import Bot._

  val token: String

  private[this] var offset = -1

  private[this] def send(methodName: String, request: HttpRequest => Future[HttpRequest]): Future[JsObject] = {
    val partialRequest = HttpRequest().withUri(s"https://api.telegram.org/bot$token/$methodName")
      .withHeaders(List(`Accept`(MediaTypes.`application/json`)))
    request(partialRequest).flatMap { Http().singleRequest(_) } flatMap { response =>
      response.status match {
        case status if status.isFailure() => throw new RuntimeException("error")
        case status => jsonUnmarshaller[JsObject].apply(response.entity)
      }
    }
  }

  private[this] def sendStrict(methodName: String, request: HttpRequest => HttpRequest): Future[JsObject] =
    send(methodName, { r: HttpRequest => Future.successful(request(r)) })

  def getMe: Future[User] =
    sendStrict("getMe", _.withMethod(HttpMethods.GET)).map { json =>
      (json \ "result").as[User]
    }

  def getUpdates(limit: Int = 100, timeout: Int = 0): Future[List[Message]] = {
    def updateRequest(request: HttpRequest): HttpRequest = {
      val uri = request.uri
      val queryUri = uri.withQuery("offset" -> (offset + 1).toString, "limit" -> limit.toString, "timeout" -> timeout.toString)
      request.withUri(queryUri).withMethod(HttpMethods.GET)
    }
    val updates = sendStrict("getUpdates", updateRequest).map { json => (json \ "result").as[List[Update]] }
    updates.map { upd =>
      offset = (offset :: upd.map(_.update_id)).max
      upd.map(_.message).collect {
        case Some(message) => message
      }
    }
  }

  def sendMessage(chat_id: Int, text: String, disable_web_page_preview: Boolean = false,
                 reply_to_message_id: Option[Int] = None, reply_markup: Option[ReplyMarkup] = None): Future[Message] = {
    def sendRequest(request: HttpRequest): Future[HttpRequest] = {
      val fields = List("chat_id" -> chat_id.toString, "text" -> text) ++
        (if (disable_web_page_preview) List("disable_web_page_preview" -> "true") else List()) ++
        reply_to_message_id.map("reply_to_message_id" -> _.toString).toList ++
        reply_markup.map(markup => "reply_markup" -> Json.stringify(ReplyMarkup.replyMarkupWrites.writes(markup))).toList
      val form: Future[MessageEntity] = Marshal(FormData(fields.toMap)).to[RequestEntity]
      form map { f => request.withMethod(HttpMethods.POST).withEntity(f) }
    }
    send("sendMessage", sendRequest).map(json => (json \ "result").as[Message])
  }

}

object Bot {

  implicit def jsonUnmarshaller[T: Reads]()(implicit fm: Materializer, ec: ExecutionContext): FromEntityUnmarshaller[T] =
    PredefinedFromEntityUnmarshallers.stringUnmarshaller.forContentTypes(`application/json`)
      .map(s => implicitly[Reads[T]].reads(Json.parse(s)).recoverTotal(e => throw new RuntimeException(e.toString)))

}


