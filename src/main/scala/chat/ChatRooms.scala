package chat

import akka.actor.{ActorRef, ActorSystem, Props}

class ChatRooms(implicit val actorSystem: ActorSystem) {

  var chatRooms: Map[String, ActorRef] = Map.empty[String, ActorRef]

  def findOrCreate(name: String): ActorRef = {
    chatRooms.getOrElse(name, createNewChatRoom(name))
  }

  private def createNewChatRoom(name: String): ActorRef = {
    val chatRoom = actorSystem.actorOf(Props[ChatRoom])

    chatRooms += name -> chatRoom
    chatRoom
  }

}
