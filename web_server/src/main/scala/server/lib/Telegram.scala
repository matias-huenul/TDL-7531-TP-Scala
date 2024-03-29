package server.lib

import akka.actor.ActorSystem
import org.json4s._
import org.json4s.jackson.Serialization.write
import org.json4s.jackson.JsonMethods._
import org.json4s.native.JsonMethods.parse
import scala.concurrent.Future
import akka.http.scaladsl.model._
import akka.stream.Materializer
import scala.concurrent.ExecutionContext

object Telegram {
  implicit val system = ActorSystem("telegram")
  implicit val formats: Formats = DefaultFormats

  case class Message(
    chatId: String,
    text: String
  )

  /** Parse a message from the Telegram API.
    *
    * @param messageBody The body of the message to parse.
    * @return A tuple containing the chat ID and the text of the message.
    */
  def parseMessage(messageBody: String): Option[Message] = {
    try {
      val jsonBody = parse(messageBody)
      val chatId = (jsonBody \ "message" \ "chat" \ "id").extract[String]
      val text = (jsonBody \ "message" \ "text").extract[String]
      Some(Message(chatId, text))
    } catch {
      case _: Exception => None
    }
  }

  /** Send a message to a Telegram chat.
    *
    * @param chatId The chat ID to send the message to.
    * @param text The text of the message to send.
    * @return A Future containing the response from the Telegram API.
    */
  def sendMessage(
    chatId: String,
    text: String
  )(implicit mat: Materializer, ec: ExecutionContext): Future[JValue] = {
    val telegramApiKey = sys.env("TELEGRAM_API_TOKEN")
    val message = Map("chat_id" -> chatId, "text" -> text, "parse_mode" -> "Markdown", "disable_web_page_preview" -> true)

    Utils.makeHttpRequest(
      s"https://api.telegram.org/bot$telegramApiKey/sendMessage",
      HttpMethods.POST,
      write(message)
    )
  }
}
