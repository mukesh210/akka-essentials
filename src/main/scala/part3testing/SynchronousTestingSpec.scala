package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import part3testing.SynchronousTestingSpec.Counter

import scala.concurrent.duration.Duration

/*
  We don't need to extend TestKit while writing Synchronous testing
 */
class SynchronousTestingSpec
  extends WordSpecLike
    with BeforeAndAfterAll {

 implicit val system = ActorSystem("SynchronousTestingSpec")

  override def afterAll(): Unit = {
    system.terminate()
  }

  import SynchronousTestingSpec._
  "A counter" should {
    "synchronously increase its counter" in {
      val counter = TestActorRef[Counter](Props[Counter])

      // it is not guaranteed that message is immediately received by Counter... but
      // because this is running in calling thread... we are sure that message is immediately
      // received by Counter
      counter ! Inc

      assert(counter.underlyingActor.count == 1)
    }

    "synchronously increase its counter at the call of the receive function" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter.receive((Inc))

      assert(counter.underlyingActor.count == 1)
    }

    "work on the calling thread dispatcher" in {
      // this way whatever message is sent to Counter, will be sent by calling thread itself
      // behaviour of this test is similar to above test... both are done by calling thread
      val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id))
      val probe = TestProbe()

      probe.send(counter, Read) // interaction happens in calling thread
      probe.expectMsg(Duration.Zero, 0) // probe has already received message 0... so this is passing
    }
  }
}

object SynchronousTestingSpec {

  case object Inc
  case object Read

  class Counter extends Actor {
    var count = 0
    override def receive: Receive = {
      case Inc => count += 1
      case Read => sender() ! count
    }
  }

}
