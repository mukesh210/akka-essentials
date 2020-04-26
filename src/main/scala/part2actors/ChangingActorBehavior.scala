package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChangingActorBehavior.Mom.MomStart

object ChangingActorBehavior extends App {

  object FussyKid {
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
  }
  class FussyKid extends Actor {
    import FussyKid._
    import Mom._
    var state = HAPPY
    override def receive: Receive = {
      case Food(VEGETABLES) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        if(state == HAPPY)
          sender() ! KidAccept
        else
          sender() ! KidReject
    }
  }

  class StatelessFussyKid extends Actor {
    import FussyKid._
    import Mom._

    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETABLES) => context.become(sadReceive, false)
      case Food(CHOCOLATE) => // stay happy
      case Ask(_) => sender() ! KidAccept
    }
    def sadReceive: Receive = {
      case Food(VEGETABLES) =>  context.become(sadReceive, false)
      case Food(CHOCOLATE) => context.unbecome()
      case Ask(_) => sender() ! KidReject
    }
  }

  object Mom {
    case class MomStart(kidRef: ActorRef)
    case class Food(food: String)
    case class Ask(message: String) // wanna play
    val VEGETABLES = "veggies"
    val CHOCOLATE = "chocolate"
  }
  class Mom extends Actor {
    import Mom._
    import FussyKid._

    override def receive: Receive = {
      case MomStart(ref) =>
        ref ! Food(VEGETABLES)
        ref ! Food(VEGETABLES)
        ref ! Food(CHOCOLATE)
        ref ! Food(CHOCOLATE)
        ref ! Ask("Wanna Play!")

      case KidAccept => println("Yayy... Kid is happy")

      case KidReject => println("Kid is sad but healthy")
    }
  }

  val system = ActorSystem("changingActorBehaviorDemo")
  val fussyKid = system.actorOf(Props[FussyKid], "fussyKid")
  val statelessFussyKid = system.actorOf(Props[StatelessFussyKid])
  val mom = system.actorOf(Props[Mom], "mom")

  mom ! MomStart(fussyKid)
  mom ! MomStart(statelessFussyKid)

  /**
    * Exercises
    *
    * 1. recreate the Counter actor with context.become and no MUTABLE state
    */
  Thread.sleep(500)
  println("----------- EXERCISE 1 ------------")

  // DOMAIN of the counter actor
  object Counter {
    case object Increment
    case object Decrement
    case object Print
    def props(): Props = Props(new Counter())
  }
  class Counter extends Actor {
    import Counter._

    override def receive: Receive = countReceive(0)

    def countReceive(value: Int): Receive = {
      case Increment =>
        println(s"[countReceive($value)] incrementing")
        context.become(countReceive(value + 1))
      case Decrement =>
        println(s"[countReceive($value)] decrementing")
        context.become(countReceive(value - 1))
      case Print => println(s"count is $value")
    }
  }
  import Counter._
  val counterActor = system.actorOf(Counter.props())
  (1 to 5).foreach(_ => counterActor ! Increment)
  counterActor ! Print
  (1 to 3).foreach(_ => counterActor ! Decrement)
  counterActor ! Print

  /**
    * Exercise 2. simplified voting system
    */

  case class Vote(candidate: String)
  case object VoteStatusRequest
  case class VoteStatusReply(candidate: Option[String])
  class Citizen extends Actor {
    override def receive: Receive = {
      case Vote(candidate) =>
        context.become(voted(candidate))
      case VoteStatusRequest => sender() ! VoteStatusReply(None)
      case _ => println("Unrecognized message")
    }

    def voted(candidate: String): Receive = {
      case VoteStatusRequest =>
        sender() ! VoteStatusReply(Some(candidate))
      case _ => println("Unrecognized message")
    }
  }

  case class AggregateVotes(citizens: Set[ActorRef])
  class VoteAggregator extends Actor {
    override def receive: Receive = awaitingCommand

    def awaitingCommand: Receive = {
      case AggregateVotes(citizens) =>
        context.become(awaitingStatuses(Map.empty[String, Int], citizens))
        citizens.foreach(citizen => citizen ! VoteStatusRequest)
    }

    def awaitingStatuses(currentStats: Map[String, Int], stillWaiting: Set[ActorRef]): Receive = {
      case VoteStatusReply(Some(candidate)) =>
        val newStats = currentStats.updated(candidate, currentStats.getOrElse(candidate, 0) + 1)
        val newStillWaiting = stillWaiting - sender()
        context.become(awaitingStatuses(newStats, newStillWaiting))
        if(newStillWaiting.isEmpty)
            println(s"All votes Received: Result: $newStats")
      case VoteStatusReply(None) =>
        println(s"${sender()} didn't voted?")
        sender() ! VoteStatusRequest
      case _ => println("Unknown message")
    }
  }

  val alice = system.actorOf(Props[Citizen])
  val bob = system.actorOf(Props[Citizen])
  val charlie = system.actorOf(Props[Citizen])
  val daniel = system.actorOf(Props[Citizen])

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  val voteAggregator = system.actorOf(Props[VoteAggregator])

  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))

  /*
    print the status of votes

    Map of candidate and vote count
    Martin -> 1
    Jonas -> 1
    Roland -> 2
   */
}
