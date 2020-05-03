package patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashDemo extends App {

  /*
    ResourceActor
      - open => receive/read/write request to the resource
      - if not open, it will postpone all requests until the state is open

      - closed
        - open => open state
        - Read/Write => messages are postponed

      - open
        - handle Read/Write
        - Close => switch to the closed state

      [Open, Read, Read, Write]
      - switch to the open state
      - read the data
      - read the data
      - write the data

      [Read, Open, Write]
      - stash Read
        Stash: [Read]
      - open => switch to Open state
        Mailbox: [Read, Write]
      - read and write are handled
   */

  case object Open
  case object Close
  case object Read
  case class Write(data: String)

  // step 1: mixin Stash trait
  class ResourceActor extends Actor with ActorLogging with Stash {
    private var innerData: String = "" // db

    override def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("Opening resource")
        // step 3: unstash all when capable of handling them
        unstashAll()  // will prepend all the stashed message in Mailbox
        context.become(open)
      case message =>
        log.info(s"Stashing $message because i can't handle it in closed state")
        // step 2: stash messages that we can't handle
        stash()
    }

    def open: Receive = {
      case Read =>
        // read data from db
        log.info(s"I have read $innerData")

      case Write(data) =>
        // write to db
        log.info(s"I am writing $data")
        innerData = data

      case Close =>
        log.info("Closing resource")
        unstashAll()
        context.become(closed)

      case message =>
        log.info(s"Stashing $message because i can't handle it in closed state")
        stash()
    }
  }

  val system = ActorSystem("StashDemo")
  val resourceActor = system.actorOf(Props[ResourceActor], "resourceActor")

//  resourceActor ! Write("I love stash")
//  resourceActor ! Read
//  resourceActor ! Open

  resourceActor ! Read
  resourceActor ! Open
  resourceActor ! Open
  resourceActor ! Write("I love stash")
  resourceActor ! Close
  resourceActor ! Read

  /**
    * stashed: [Open, ]
    * mailbox: [Read, Write, Close, Open]
    */

}
