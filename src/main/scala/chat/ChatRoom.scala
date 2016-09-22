package chat

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.{FlowShape, OverflowStrategy}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}

object ChatRoom {
  case object Join
  case class ChatMessage(username: String, message: String)
//  def apply(roomId: Int)(implicit actorSystem: ActorSystem) = new ChatRoom(roomId, actorSystem)
}

class ChatRoom extends Actor {
  import ChatRoom._

  var users: Set[ActorRef] = Set.empty[ActorRef]

  def receive = {
    case Join =>
      users += sender()
      context.watch(sender())

    case Terminated(user) => users -= user

    case msg: ChatMessage => users.filterNot(_ == sender()).foreach(_ ! msg)
  }

//  import chat.ChatRoomActor._
//
//  private[this] val chatRoomActor = actorSystem.actorOf(Props(classOf[ChatRoomActor], roomId))
//
//  def webSocketFlow(user: String): Flow[Message, Message, _] = {
//    val sdpSource: Source[ChatMessage, ActorRef] = Source.actorRef[ChatMessage](bufferSize = 5, OverflowStrategy.fail)
//
//    Flow.fromGraph(GraphDSL.create(sdpSource) {
//      implicit builder => {
//        chatSource => {
//          //flow used as input, it takes Messages
//          val fromWebsocket = builder.add(
//            Flow[Message].collect {
//              case TextMessage.Strict(txt) => IncomingMessage(user, txt)
//            })
//
//          //flow used as output, it returns Messages
//          val backToWebsocket = builder.add(
//            Flow[ChatMessage].map {
//              case ChatMessage(author, text) => TextMessage(s"[$author]: $text")
//            }
//          )
//
//          // expose ports
//          FlowShape.of(fromWebsocket.in, backToWebsocket.out)
//        }
//      }
//    })
//
//  }
//
//  def sendMessage(message: ChatMessage): Unit = chatRoomActor ! message

}
