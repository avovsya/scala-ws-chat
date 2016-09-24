package chat

import akka.actor.ActorRef
import chat.User.{OutgoingMessage, UserData}

object ChatSystem {

  def sendUserSystemMessage(userData: UserData, text: String) = {
    userData.outgoingActor ! OutgoingMessage(s"System: $text")
  }

  def broadcastSystemMessage(chatRoom: ActorRef, text: String) = {
    chatRoom ! ChatRoom.BroadcastSystemMessage(text)
  }

}
