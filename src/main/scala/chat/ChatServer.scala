package chat

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}

import scala.io.StdIn

object ChatServer extends App {

  import akka.http.scaladsl.server.Directives._

  implicit val actorSystem = ActorSystem("akka-system")
  implicit val flowMaterializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher

  val interface = "localhost"
  val port = 8000

  val echoService: Flow[Message, Message, _] = Flow[Message].map {
    case TextMessage.Strict(txt) => TextMessage("ECHO: " + txt)
    case _ => TextMessage("Message type unsupported")
  }

  val route = get {
    pathEndOrSingleSlash {
      complete("Welcome to Tic-Tac-Toe")
    } ~
    path("ws-echo") {
      get {
        handleWebSocketMessages(echoService)
      }
    } ~
    pathPrefix("ws-chat" / IntNumber) { chatId =>
      parameter('name) { userName =>
        handleWebSocketMessages(ChatRooms.findOrCreate(chatId).webSocketFlow(userName))
      }
    }
  }

  val binding = Http().bindAndHandle(route, interface, port)
  println(s"Server is listening on http://$interface:$port\nPress RETURN to stop...")

  StdIn.readLine()

  binding.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())
  println("Server is down...")
}
