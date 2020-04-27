package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object ActorLoggingDemo extends App {

  // #1 - explicit logger
  class SimpleActorWithExplicitLogger extends Actor {
    val logger = Logging(context.system, this)

    override def receive: Receive = {
      /*
        1. DEBUG
        2. INFO
        3. WARNING
        4. ERROR
       */
      case message => logger.info(message.toString)
    }
  }

  val system = ActorSystem("LoggingDemo")
  val simpleActor = system.actorOf(Props[SimpleActorWithExplicitLogger], "explicitLogger")

  simpleActor ! "Logging a simple message"

  // #2 - ActorLogging
  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case (a, b) => log.info("Two things: {} and {}", a, b)
      case message => log.info(message.toString)
    }
  }

  val actorWithLogging = system.actorOf(Props[ActorWithLogging], "actorWithLogging")
  actorWithLogging ! "Logging message by extending ActorLogging trait"

  actorWithLogging ! (42, 65)
}
