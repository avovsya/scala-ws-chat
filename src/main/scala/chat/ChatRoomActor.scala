package chat

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive

object ChatRoomActor {
  case class ChatMessage(name: String, message: String)
  case class SystemMessage(override val message: String) extends ChatMessage("System", message)
  case class IncomingMessage(override val name: String,
                             override val message: String) extends ChatMessage(name, message)

  case class UserJoined(name: String, actorRef: ActorRef)
  case class UserLeft(name: String)
}

class ChatRoomActor(roomId: Int) extends Actor {
  import ChatRoomActor._

  var participants: Map[String, ActorRef] = Map.empty[String, ActorRef]

  def broadcast(message: ChatMessage) = participants.values.foreach(_ ! message)

  override def receive: Receive = {
    case UserJoined(name, actorRef) => {
      participants += name -> actorRef
      broadcast(SystemMessage(s"User $name joined channel..."))
      println(s"User $name joined room $roomId")
    }

    case UserLeft(name) => {
      println(s"User $name left room $roomId")
      broadcast(SystemMessage(s"User $name left channel..."))
      participants -= name
    }

    case msg: IncomingMessage => broadcast(msg)
    case msg: SystemMessage => broadcast(msg)
  }
}
