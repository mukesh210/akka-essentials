package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ActorCapabilities.BankAccount.{Deposit, Statement, Withdraw}
import part2actors.ActorCapabilities.Counter.{Decrement, Increment, Print}
import part2actors.ActorCapabilities.Person.LiveTheLife

import scala.collection.mutable.ListBuffer

object ActorCapabilities extends App {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "Hi!" => context.sender() ! "Hello, there!"  // replying to message
      case x: String => println(s"[$self] I have received: ${x}")
      case number: Int => println(s"[simple actor] I have received a Number ${number}")
      case SpecialMessage(content) => println(s"[simple actor] I have received something special: ${content}")
      case SendMessageToYourSelf(content) => self ! content
      case SayHiTo(ref) => ref ! "Hi!"
      case WirelessPhoneMessage(content, ref) => ref forward (content + "s")  // i keep original sender of WPM
    }
  }

  val actorSystem = ActorSystem("actorSystemCapabilities")
  val simpleActor = actorSystem.actorOf(Props[SimpleActor], "simpleActor")
  simpleActor ! "hello, Actor"

  // 1- messages can be of any type
  // a) messages must be IMMUTABLE
  // b) messages must be SERIALIZABLE
  // in practice, use case classes and case objects
  simpleActor ! 42  // who is the sender

  case class SpecialMessage(content: String)
  simpleActor ! SpecialMessage("some special content")

  // 2 - actors have information about their context and about themselves
  // context.self === this

  case class SendMessageToYourSelf(content: String)

  simpleActor ! SendMessageToYourSelf("I am an actor")

  // 3 - actors can REPLY to messages
  val alice = actorSystem.actorOf(Props[SimpleActor], "alice")
  val bob = actorSystem.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)

  // 4 - if there is no sender, reply goes to deadletter
  alice ! "Hi!"

  // 5 - forwarding messages(originator would remain same)
  // sending message with original sender

  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("Hi", bob) // no sender

  /**
    * EXERCISES:
    *
    * 1. a Counter actor
    *   internal variable
    *     - Increment
    *     - Decrement
    *     - Print
    *
    * 2. a Bank account as an actor
    *   receives:
    *   - Deposit an amount
    *   - Withdraw an amount
    *   - Statement
    *   replies with
    *   - Success
    *   - Failure
    *
    *   interact with some other kind of actor
    */
  Thread.sleep(500)
  println("------------ ************* ----------------")

  // DOMAIN of the counter actor
  object Counter {
    case object Increment
    case object Decrement
    case object Print
    def props(): Props = Props(new Counter())
  }
  class Counter extends Actor {
    var count = 0
    override def receive: Receive = {
      case Increment => count += 1
      case Decrement => count -= 1
      case Print => println(s"Count is: ${count}")
    }
  }
  val counterActor = actorSystem.actorOf(Counter.props())
  (1 to 5).foreach(_ => counterActor ! Increment)
  counterActor ! Print
  (1 to 3).foreach(_ => counterActor ! Decrement)
  counterActor ! Print

  Thread.sleep(500)
  println("--------- *********** ------------")
  object BankAccount {
    case class Withdraw(amount: Int)
    case class Deposit(amount: Int)
    case object Statement
    case class TransactionSuccess(message: String)
    case class TransactionFailure(reason: String)

    def props(): Props = Props(new BankAccount())
  }
  class BankAccount extends Actor {
    import BankAccount._
    var funds = 0

    override def receive: Receive = {
      case Deposit(amount) =>
        if(amount < 0)
          sender() ! TransactionFailure("invalid deposit amount")
        else {
          funds += amount
          sender() ! TransactionSuccess(s"successfully deposited $amount")
        }

      case Withdraw(amount) =>
        if(amount < 0)
          sender() ! TransactionFailure("invalid withdraw amount")
        else if(amount > funds)
          sender() ! TransactionFailure("insufficient funds")
        else {
            funds -= amount
            sender() ! TransactionSuccess(s"successfully withdrew $amount")
          }

      case Statement => sender() ! s"Your balance is $funds"
    }
  }

  object Person {
    case class LiveTheLife(account: ActorRef)
  }
  class Person extends Actor {
    import Person._

    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! Deposit(10000)
        account ! Withdraw(9000000)
        account ! Withdraw(500)
        account ! Statement

      case message => println(message.toString)
    }
  }

  val account = actorSystem.actorOf(BankAccount.props(), "bankAccount")
  val person = actorSystem.actorOf(Props[Person], "bobBillionaire")

  person ! LiveTheLife(account)
}
