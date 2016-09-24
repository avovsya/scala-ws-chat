package chat

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props, Terminated}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.{FlowShape, OverflowStrategy}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}

import scala.collection.immutable.Queue

object ChatRoom {
  case object Join
  case object Leave
  case class BroadcastMessage(username: String, text: String, sentBy: ActorRef)
  case class BroadcastSystemMessage(text: String)
}

class ChatRoom extends Actor {
  import ChatRoom._

  var users: Set[ActorRef] = Set.empty[ActorRef]
  var history: Queue[User.OutgoingMessage] = Queue.empty[User.OutgoingMessage]

  def receive = {
    case Join =>
      users += sender()
      context.watch(sender())
      // If the history is relatively big(~ 10-20 messages)
      // This will throw "WebSocket handler failed with Buffer overflow (max capacity was: 10)!"
      history.foreach(sender() ! _)

    case Leave =>
      users -= sender()

    case Terminated(user) => users -= user

    case msg @ BroadcastMessage(username, text, sendBy) => {
      val outgoingMessage = User.OutgoingMessage(s"$username: $text")
      history = history.enqueue(outgoingMessage)
      users.filterNot(_ == sendBy) foreach (_ ! outgoingMessage)
    }

    case msg @ BroadcastSystemMessage(text) => {
      val outgoingMessage = User.OutgoingMessage(text)
      history = history.enqueue(outgoingMessage)
      users foreach (_ ! outgoingMessage)
    }
  }
}
