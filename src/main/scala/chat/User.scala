package chat

import akka.actor.{Actor, ActorRef, FSM}

object User {
  case class Connect(outgoing: ActorRef)

  case class IncomingMessage(text: String)

  sealed trait OutgoingMessage {
    val username: String
    val text: String
  }
  case class UserMessage(username: String, text: String) extends OutgoingMessage
  case class SystemMessage(text: String) extends OutgoingMessage {
    override val username: String = "System"
  }

  sealed trait State
  case object Disconnected extends State
  case object Connected extends State
  case object Chatting extends State

  sealed trait Data
  case object EmptyData extends Data
  case class UserData(name: String, outCh: ActorRef) extends Data
}

class User(chatRoom: ActorRef) extends FSM[User.State, User.Data] {
  import User._

  startWith(Disconnected, EmptyData)

  when(Disconnected) {
    case Event(Connect(outgoingActor), _) =>
      println(s"User connected $self")
      outgoingActor ! SystemMessage("Welcome User! Please enter your name:")
      goto(Connected) using(UserData("", outgoingActor))
  }

  when(Connected) {
    case Event(IncomingMessage(text), UserData(_, outCh)) =>
      println(s"User $self selected name $text")
      outCh ! SystemMessage(s"Nice to meet you $text. Please enjoy this chat")
      chatRoom ! ChatRoom.Join
      chatRoom ! ChatRoom.ChatMessage("System", s"User $text joined the chat")
      goto(Chatting) using(UserData(text, outCh))
  }

  when(Chatting) {
    case Event(IncomingMessage(text), userData @ UserData(username, outCh)) =>
      chatRoom ! ChatRoom.ChatMessage(username, text)
      stay using userData

    case Event(ChatRoom.ChatMessage(username, text), userData @ UserData(_, outCh)) =>
      outCh ! UserMessage(username, text)
      stay using userData
  }

//  def receive = {
//
//    case Connected(outgoing) => {
//      context.become(connected(outgoing))
//    }
//
//  }

//  def connected(outgoing: ActorRef): Receive = {
//    chatRoom ! ChatRoom.Join
//
//    {
//      case IncomingMessage(text) =>
//        chatRoom ! ChatRoom.ChatMessage(text)
//
//      case ChatRoom.ChatMessage(text) =>
//        outgoing ! OutgoingMessage(text)
//    }
//  }

}
