package net.rfc1149.rxtelegram

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorSystem, Stash}
import akka.pattern.pipe
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.rfc1149.rxtelegram.Bot.{Send, Target}
import net.rfc1149.rxtelegram.model._
import net.rfc1149.rxtelegram.model.media.Media

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

abstract class ActorBot(val token: String, val config: Config = ConfigFactory.load()) extends Actor with ActorLogging with Stash with Bot {

  import ActorBot._

  implicit val actorSystem: ActorSystem = context.system
  implicit val ec: ExecutionContext = context.dispatcher
  implicit val fm: Materializer = ActorMaterializer()

  private[this] val httpErrorRetryDelay = config.as[FiniteDuration]("rxtelegram.http-error-retry-delay")
  private[this] val longPollingDelay = config.as[FiniteDuration]("rxtelegram.long-polling-delay")

  protected[this] var me: User = _

  protected[this] def handleMessage(message: Message): Unit

  protected[this] def handleOther(other: Any): Unit = {
    log.info(s"received unknown content: $other")
  }

  override def preStart() =
    self ! GetMyself

  override def receive = {
    case GetMyself =>
      getMe.pipeTo(self)

    case user: User =>
      me = user
      setWebhook("")
      unstashAll()
      context.become(receiveIKnowMe)
      self ! Replenish

    case Failure(t) =>
      log.error(t, "error when getting information about myself")
      context.system.scheduler.scheduleOnce(httpErrorRetryDelay, self, GetMyself)

    case other =>
      stash()
  }

  def receiveIKnowMe: Receive = {
    case GetMe =>
      sender ! me

    case Updates(updates) =>
      for (update <- updates) {
        try {
          update.message.foreach(handleMessage)
        } catch {
          case t: Throwable =>
            log.error(t, s"exception when handling ${update.message.get}")
        }
        acknowledgeUpdate(update)
      }
      self ! Replenish

    case UpdatesError(throwable) =>
      log.error(throwable, "error while getting updates")
      context.system.scheduler.scheduleOnce(httpErrorRetryDelay, self, Replenish)

    case Replenish =>
      replenish()

    case data: Send =>
      send(data) pipeTo sender()

    case SetWebhook(uri, certificate) =>
      setWebhook(uri, certificate).pipeTo(sender())

    case GetUserProfilePhotos(user, offset, limit) =>
      getUserProfilePhotos(user.id, offset, limit).pipeTo(sender())

    case ForwardMessage(target, origin) =>
      forwardMessage(target.chat_id, origin.chat.id, origin.message_id).pipeTo(sender())

    case other =>
      try {
        handleOther(other)
      } catch {
        case t: Throwable =>
          log.error(t, s"error when handling $other")
      }
  }

  private[this] def replenish(): Unit =
    getUpdates(timeout = longPollingDelay).map(Updates).recover { case throwable => UpdatesError(throwable) }.pipeTo(self)

}

object ActorBot {

  case object GetMe
  private[ActorBot] case object Replenish
  private[ActorBot] case object GetMyself
  case class Updates(updates: List[Update])
  private[ActorBot] case class UpdatesError(throwable: Throwable)


  case class SetWebhook(uri: String = "", certificate: Option[Media] = None)

  case class GetUserProfilePhotos(user: User, offset: Long = 0, limit: Long = 100)

  case class ForwardMessage(target: Target, origin: Message)

  private[ActorBot] case class MediaParameter(fieldName: String, media: Media) {
    def toBodyPart = media.toBodyPart(fieldName)
  }

}
