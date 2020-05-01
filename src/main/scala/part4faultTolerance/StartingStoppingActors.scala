package part4faultTolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}

object StartingStoppingActors extends App {

  val system = ActorSystem("StoppingActorDemo")

  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object Stop
  }

  class Parent extends Actor with ActorLogging {
    import Parent._
    override def receive: Receive = withChildren(Map())

    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"Starting Child ${name}")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))

      case StopChild(name) =>
        log.info(s"Stopping child with the name $name")
        val childOption = children.get(name)
        // stopping actor is async
        childOption.foreach(childRef => context.stop(childRef))

      case Stop =>
        log.info("Stopping myself")
        context.stop(self)

      case message => log.info(message.toString)
    }
  }

  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /**
    * method #1 - using context.stop
    */
  import Parent._

  val parent = system.actorOf(Props[Parent], "parent")
  parent ! StartChild("child1")
  val child = system.actorSelection("/user/parent/child1")
  child ! "Hi Kid!"

  parent ! StopChild("child1")

  // for(_ <- 1 to 50) child ! "are you still alive?"

  parent ! StartChild("child2")
  val child2 = system.actorSelection("/user/parent/child2")
  child2 ! "Hi, second child"

  parent ! Stop
  for(_ <- 1 to 10) parent ! "parent, are you still there?" // should not be received
  for(i <- 1 to 100) child2 ! s"[${i}] second kid, are you still alive?" // // should not be received

  /**
    * method #2: using special messages
    */
  val looseActor = system.actorOf(Props[Child], "looseActor")
  looseActor ! "hello, loose actor"
  looseActor ! PoisonPill
  looseActor ! "loose actor, are you still there?"

  val abruptlyTerminatedActor = system.actorOf(Props[Child], "abruptActor")
  abruptlyTerminatedActor ! "you are about to be terminated"
  abruptlyTerminatedActor ! Kill  // will kill actor and throw ActorKilledException
  abruptlyTerminatedActor ! "you have been terminated"

  /**
    * Death watch
    */
  class Watcher extends Actor with ActorLogging {
    import Parent._
    override def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"Started and Watching child ${name}")
        // not necessary that watchedActor is child of this actor...
        context.watch(child)
      case Terminated(ref) =>
        log.info(s"the reference that i'm watching ${ref} has been stopped")
    }
  }

  val watcher = system.actorOf(Props[Watcher], "watcher")
  watcher ! StartChild("watchedChild")
  val watchedChild = system.actorSelection("/user/watcher/watchedChild")
  Thread.sleep(500)

  watchedChild ! PoisonPill
}
