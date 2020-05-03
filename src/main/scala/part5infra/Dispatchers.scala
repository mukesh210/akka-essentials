package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

/**
  * Dispatchers are responsible for delivering and handling messages within Actor System
  */
object Dispatchers extends App {

  class Counter extends Actor with ActorLogging {
    var count = 0

    override def receive: Receive = {
      case message =>
        count += 1
        log.info(s"[${count}] $message")
    }
  }

  // ConfigFactory.load().getConfig("dispatchersDemo")
  val system = ActorSystem("DispatchersDemo")

  // method #1 - attaching dispatcher to Actor - programatically
  val actors = for(i <- 1 to 10) yield
    system.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter_${i}")

  val r = new Random()
//  for(i <- 1 to 1000) {
//    actors(r.nextInt(10)) ! i
//  }

  // method #2: from config(name of actor should be same as one in config file)
  val rtjvmActor = system.actorOf(Props[Counter], "rtjvm")

  /**
    * Dispatchers implement the ExecutionContext trait
    */

  class DBActor extends Actor with ActorLogging {
    // implicit val executionContext: ExecutionContext = context.dispatcher
    // Solution #1: using different dispatcher for blocking calls
    implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("my-dispatcher")

    // Solution #2: use Router

    override def receive: Receive = {
      case message => Future {
        // wait on a resource
        Thread.sleep(5000)
        log.info(s"Success: ${message}")
      }
    }
  }

  val dbActor = system.actorOf(Props[DBActor])
  //dbActor ! "meaning of life is 42"
  /**
    * NOTE: Running of Futures/Blocking call in actor is discouraged
    * because Dispatcher won't be able to use this thread for other activities
    *
    */

  /**
    * Running below will output results in batches because we have only 1 thread and when dbActor is being processed,
    * it blocks thread... as a result of which, we are seeing results in batches
    * Solution: USE DIFFERENT DISPATCHER FROM CONF
    */
  val nonBlockingActor = system.actorOf(Props[Counter])
  for(i <- 1 to 1000) {
    val message = s"important message $i"
    dbActor ! message
    nonBlockingActor ! message
  }

}
