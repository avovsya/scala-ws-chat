package chat

import akka.actor.{ActorRef, FSM}

object User {
  case class Connect(outgoing: ActorRef)

  sealed trait State
  case object Disconnected extends State
  case object ChooseUsername extends State
  case object ChooseRoom extends State
  case object Chat extends State

  sealed trait Data
  case object EmptyData extends Data
  case class UserData(name: String, chatRoom: ActorRef, outgoingActor: ActorRef) extends Data

  case class IncomingMessage(text: String)
  case class OutgoingMessage(text: String)
}

class User(chatRooms: ChatRooms) extends FSM[User.State, User.Data] {
  import User._

  def sendMessage(userData: UserData, outgoingMessage: OutgoingMessage) = {
    userData.outgoingActor ! outgoingMessage
  }

  def chooseRoom(userData: UserData): State = {
    ChatSystem.sendUserSystemMessage(userData, s"Please enter chat room that you would like to join")
    goto(ChooseRoom) using userData
  }

  startWith(Disconnected, EmptyData)

  when(Disconnected) {
    case Event(Connect(outgoingActor), _) =>
      val userData = UserData("", null, outgoingActor)
      ChatSystem.sendUserSystemMessage(userData, "Welcome User! Please enter your name:")
      goto(ChooseUsername) using userData
  }

  when(ChooseUsername) {
    case Event(IncomingMessage(text), userData @ UserData(_, _, outgoingActor)) =>
      ChatSystem.sendUserSystemMessage(userData, s"Nice to meet you $text. Please enjoy this chat")
      chooseRoom(UserData(text, null, outgoingActor))
  }

  when(ChooseRoom) {
    case Event(IncomingMessage(text), userData @ UserData(name, _, outgoingActor)) =>
      ChatSystem.sendUserSystemMessage(userData, s"Welcom to $text room")

      val chatRoom: ActorRef = chatRooms.findOrCreate(text)

      chatRoom ! ChatRoom.Join
      goto(Chat) using UserData(name, chatRoom, outgoingActor)
  }

  when(Chat) {
    case Event(IncomingMessage("/leave"), userData @ UserData(name, chatRoom, _)) =>
      ChatSystem.broadcastSystemMessage(userData.chatRoom, s"User $name left the chat")
      chatRoom ! ChatRoom.Leave
      chooseRoom(userData)

    case Event(IncomingMessage(text), userData @ UserData(username, chatRoom, outCh)) =>
      chatRoom ! ChatRoom.BroadcastMessage(username, text, self)
      stay using userData

    case Event(msg: OutgoingMessage, userData: UserData) =>
      sendMessage(userData, msg)
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
