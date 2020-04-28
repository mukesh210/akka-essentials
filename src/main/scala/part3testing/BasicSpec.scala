package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import part3testing.BasicSpec.{BlackHole, EchoActor, LabTestActor}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
 with ImplicitSender
 with WordSpecLike
 with BeforeAndAfterAll {

  // setup
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  // test-suite
  "EchoActor" should {
    // test
    "send back the same message" in {
      val echoActor = system.actorOf(Props[EchoActor])
      val message = "hello, tests"
      /*
        ! message takes `self: ActorRef` as second argument and is implicit
        `testActor` is present in TestKitBase and is the actor responsible for
        sending and receiving messages from Actor

        `ImplicitSender` trait does nothing but assign above `testActor` to implicit parameter
       */
      echoActor.!(message)

      expectMsg(message) // akka.tests.single-expect-default
    }
  }

  "A black hole actor" should {
    "send back the same message" in {
      val blackHole = system.actorOf(Props[BlackHole])
      val message = "hello, tests"
      blackHole ! message

      expectNoMessage(1 second)
    }
  }

  // message assertions
  "A lab test actor" should {
    val labTestActor = system.actorOf(Props[LabTestActor])
    "turn a string into uppercase" in {
      labTestActor ! "I love Akka"
      // expectMsg("I LOVE AKKA") OR
      // expectMsgType -> get hold of message
      val reply: String = expectMsgType[String]
      assert(reply == "I LOVE AKKA")
    }

    "reply to a greeting" in {
      labTestActor ! "greeting"
      expectMsgAnyOf("hi", "hello")
    }

    "reply with favouriteTech" in {
      labTestActor ! "favouriteTech"
      expectMsgAllOf("Scala", "Akka")
    }

    "reply with cool tech in a different way" in {
      labTestActor ! "favouriteTech"
      val messages: immutable.Seq[AnyRef] = receiveN(2)

      // free to do more complicated assertions
    }

    "reply with cool tech in a fancy way" in {
      labTestActor ! "favouriteTech"
      expectMsgPF() {
        case "Scala" => // only care that PF is defined
        case "Akka" =>
      }
    }
  }
}

// will store information for our tests
object BasicSpec {

  class EchoActor extends Actor {
    override def receive: Receive = {
      case message => sender() ! message
    }
  }

  class BlackHole extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }

  class LabTestActor extends Actor {
    val random = new Random

    override def receive: Receive = {
      case "greeting" =>
        if(random.nextBoolean()) sender ! "hi" else sender() ! "hello"
      case "favouriteTech" =>
        sender() ! "Scala"
        sender() ! "Akka"
      case message: String => sender() ! message.toUpperCase
    }
  }

}
