package chat

import akka.NotUsed
import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import scala.concurrent.duration
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.io.StdIn

object ChatServer extends App {

  import akka.http.scaladsl.server.Directives._

  implicit val actorSystem = ActorSystem("akka-system")
  implicit val flowMaterializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher

  val interface = "localhost"
  val port = 8000

//  val chatRoom = actorSystem.actorOf(Props(new ChatRoom), "chat")
  val chatRooms = new ChatRooms

  // WebSocket Flow
  //
  //  Incoming WS message                                 Outgoing WS Message
  // +-------------------->Flow[Message(In), Message(Out)]+----------------->
  //
  def chatFlow: Flow [Message, Message, _] = {

    val userActor = actorSystem.actorOf(Props(new User(chatRooms)))

    val incomingMessages: Sink[Message, NotUsed] = Flow[Message].map {
      // transform websocket message to domain message
      case TextMessage.Strict(text) => User.IncomingMessage(text)
    }.to(Sink.actorRef[User.IncomingMessage](userActor, PoisonPill)) // Use userActor as Sink for the Incoming messages

    val outgoingMessages: Source[Message, NotUsed] = {
      // This Source creates ActorRef when materialized
      // When this ActorRef gets User.OutgoingMessage -
      // Source will map that message to WebSocket TextMessage class
      Source.actorRef[User.OutgoingMessage](bufferSize = 10, OverflowStrategy.fail)
        .mapMaterializedValue({
          outActor => // get ActorRef that will be materialized by this source
            userActor ! User.Connect(outActor) // and pass it to userActor
            NotUsed
        })
        .map((outgoingMessage: User.OutgoingMessage) => TextMessage(outgoingMessage.text))
    }
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }

  val route = get {
    pathEndOrSingleSlash {
      complete("Welcome to Tic-Tac-Toe")
    } ~
    pathPrefix("ws-chat") {
      handleWebSocketMessages(chatFlow)
    }
  }

  val binding = Http().bindAndHandle(route, interface, port)
  println(s"Server is listening on http://$interface:$port\nPress RETURN to stop...")

  StdIn.readLine()

  binding.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())
  println("Server is down...")
}
