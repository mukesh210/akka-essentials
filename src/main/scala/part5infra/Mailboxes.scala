package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Develop our own mailbox and later see how to configure default one provided by Akka
  */
object Mailboxes extends App {

  val system = ActorSystem("MailboxDemo", ConfigFactory.load().getConfig("mailboxesDemo"))

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /**
    * Interesting call #1 - custom Priority mailbox(Ticketing system)
    * P0 -> most important
    * P1, P2, P3
    */

  // step #1: mailbox definition
  class SupportTicketPriorityMailbox(settings: ActorSystem.Settings, config: Config)
  extends UnboundedPriorityMailbox(
    PriorityGenerator {
      case message: String if message.startsWith("[P0]") => 0
      case message: String if message.startsWith("[P1]") => 1
      case message: String if message.startsWith("[P2]") => 2
      case message: String if message.startsWith("[P3]") => 3
  })

  // step #2 - make it known to config
  // step #3 - attach the dispatcher to an actor

  val supportTicketLogger = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"))
//  supportTicketLogger ! "[P3] this thing would be nice to have"
//  supportTicketLogger ! "[P0] need to be solved now"
//  supportTicketLogger ! "[P1] do this when you have time"
  // after which time can i send another message and be prioritized accordingly?

  /**
    * Interesting case #2: control-aware mailbox(some message need to be pickup up irrespective
    * of what is first in Queue)
    * we will use UnboundedControlAwareMailbox
    */

  // step 1: mark important messages as control messages
  case object ManagementTicket extends ControlMessage

  /*
  step 2: configure who gets the mailbox
   - make the actor attach to the mailbox
   */

  // method #1: attaching an actor with control mailbox
  val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"))
//  controlAwareActor ! "[P3] this thing would be nice to have"
//  controlAwareActor ! "[P0] need to be solved now"
//  controlAwareActor ! "[P1] do this when you have time"
//  controlAwareActor ! ManagementTicket

  // method #2: using deployment config
  val altControlAwareActor = system.actorOf(Props[SimpleActor], "altControlAwareActor")
  altControlAwareActor ! "[P3] this thing would be nice to have"
  altControlAwareActor ! "[P0] need to be solved now"
  altControlAwareActor ! "[P1] do this when you have time"
  altControlAwareActor ! ManagementTicket


}
