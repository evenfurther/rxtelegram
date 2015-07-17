package net.rfc1149.rxtelegram

import akka.actor.ActorSystem
import net.rfc1149.rxtelegram.model.{Message, User}

import scala.concurrent.Await
import scala.concurrent.duration._

object Test extends App {

  val actorSystem = ActorSystem("bot")

  val bot = new Bot {
    val token = "82495840:AAHpI1B06UnJk8jTJxKKy5bNzVPJJhzJKNo"
    val actorSystem = Test.actorSystem
  }

  val me: User = Await.result(bot.getMe, 5.seconds)
  println(me)

  var updates: List[Message] = Nil

  do {
    updates = Await.result(bot.getUpdates(timeout = 2), 3.seconds)

    for (message <- updates) {
      println(message)
      val answer = Await.result(bot.sendMessage(message.chat.id, "Yeah, I read that: " + message.text.getOrElse("<nothing>"),
      reply_to_message_id = Some(message.message_id)), 1.second)
      println(answer)
    }

  } while (updates.nonEmpty)

  Thread.sleep(1000)

  actorSystem.shutdown()

}