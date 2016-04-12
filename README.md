# Telegram Bot API for Scala

This implements the [Telegram bot API](https://core.telegram.org/bots/api) for Scala. It uses Akka to build
reactive bots.

The API is implemented up to the changes of April 12, 2016 (Bot 2.0)
(see [Telegram bot API changelog](https://core.telegram.org/bots/api-changelog)),
except for the `InlineQueryResultCached…` messages.
