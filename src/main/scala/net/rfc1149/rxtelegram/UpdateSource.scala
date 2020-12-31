package net.rfc1149.rxtelegram

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{RestartSource, Source}
import net.rfc1149.rxtelegram.model._

import scala.concurrent.Future
import akka.stream.RestartSettings

object UpdateSource {

  private class UpdatesIterator(val token: String, val options: Options)(implicit val actorSystem: ActorSystem) extends Bot with Iterator[Future[List[Update]]] {
    implicit val ec = actorSystem.dispatcher
    override def hasNext: Boolean = true
    override def next(): Future[List[Update]] =
      getUpdates(limit   = options.updatesBatchSize, timeout = options.longPollingDelay)
        .map { l => l.lastOption.foreach(acknowledgeUpdate); l }
  }

  private def source(token: String, options: Options)(implicit system: ActorSystem): Source[Update, NotUsed] =
    Source.fromIterator(() => new UpdatesIterator(token, options))
      .mapAsync(parallelism = 1)(identity)
      .mapConcat(identity)

  def apply(token: String, options: Options)(implicit system: ActorSystem): Source[Update, NotUsed] = {
    val restartSettings = RestartSettings(options.httpMinErrorRetryDelay, options.httpMaxErrorRetryDelay, 0.2)
    RestartSource.withBackoff(restartSettings) {
      () => source(token, options)
    }
  }

}

