package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.collection.immutable

object ChildActorsExercise extends App {

  // doing distributed word counting

  object WordCounterMaster {
    case class Initialize(nChildren: Int) // create n child of type WordCounterWorker
    case class WordCountTask(taskId: Int, text: String)
    case class WordCountReply(taskId: Int, count: Int)
  }
  class WordCounterMaster extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case Initialize(nChildren) =>
        println(s"[WordCountMaster] Initializing $nChildren...")
        val childList: immutable.Seq[ActorRef] = (1 to nChildren).map(x =>
          context.actorOf(Props[WordCounterWorker], s"child${x}")
        )
        context.become(withChildren(childList))
    }

    def withChildren(refs: Seq[ActorRef],
                      currentChildIndex: Int = 0,
                      currentTaskId: Int = 0,
                      requestMap: Map[Int, ActorRef] = Map()): Receive = {
      case text: String =>
        println(s"[WordCountMaster] Received ${text}: sending to ${currentChildIndex}th children...")
        val latestTaskId = currentTaskId + 1
        refs(currentChildIndex) ! WordCountTask(latestTaskId, text)
        context.become(withChildren(refs,
          (currentChildIndex + 1) % refs.length,
          latestTaskId,
          requestMap + (latestTaskId -> sender())
        ))

      case WordCountReply(taskId, count) =>
        println(s"[WordCountMaster] received reply for ${taskId} with ${count}")
        val originalSender = requestMap(taskId)
        originalSender ! count
        context.become(withChildren(refs, currentChildIndex, currentTaskId,
          requestMap - taskId))
    }
  }

  class WordCounterWorker extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"[${self.path.name}] Received id:${id} :: text: $text")
        val length = text.split(" ").length
        sender() ! WordCountReply(id, length)
    }
  }

  class TestActor extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case "go" =>
        val master = system.actorOf(Props[WordCounterMaster], "master")
        master ! Initialize(3)
        val list = List(
          "scala is awesome",
          "akka is fun and easy",
          "kafka topics, brokers, zookeepers",
          "don't compare java to scala hahahaha",
          "learning akka",
          "after this learn akka streams",
          "akka http is also fun! isn't it"
        )
        list.foreach(text => master ! text)
      case count: Int =>
        println(s"[test actor] I have received a reply ${count}")
    }
  }

  val system = ActorSystem("ChildActorExercise")
  val testActor = system.actorOf(Props[TestActor], "testActor")
  testActor ! "go"
  /*
    create WordCounterMaster
      send Initialize(10) to WordCounterMaster
        -- initialize 10 worker

      send "Akka is awesome" to Master
        - send a WordCountTask to one of it's children
          - child replies with Reply(3) to master
      - master replies to sender with 3

      requester -> wcm -> wcw
                <- wcr <-
       // round-robin logic
   */

}
