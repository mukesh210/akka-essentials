package part4faultTolerance

import java.io.File

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{Backoff, BackoffSupervisor}

import scala.io.Source
import scala.concurrent.duration._

/**
  * Problem: When connecting to DB... if actor fails,
  * it will be restarted but it will fail again because db will take
  * some time to start... also, if lots of them are trying to connect
  * at same time, db might fail again due to load...
  *
  * Solution: Backoff... start after some random time
  */
object BackoffSupervisorPattern extends App {

  case object ReadFile
  class FileBasedPersistentActor extends Actor with ActorLogging {
    var dataSource: Source = null

    override def preStart(): Unit =
      log.info("Persistent actor starting...")

    override def postStop(): Unit =
      log.warning("Persistent actor has stopped...")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.info("Persistent actor restarting...")

    override def receive: Receive = {
      case ReadFile =>
        if(dataSource == null)
          dataSource = Source.fromFile(new File("src/main/resources/testfiles/important.txt"))
        log.info("I've just read some IMPORTANT data: " + dataSource.getLines().toList)
    }
  }

  val system = ActorSystem("BackoffSupervisorsDemo")
//  val simpleActor = system.actorOf(Props[FileBasedPersistentActor], "simpleActor")
//
//  simpleActor ! ReadFile  // will normally... will output file content

  val simpleSupervisorProps = BackoffSupervisor.props(
    Backoff.onFailure(
      Props[FileBasedPersistentActor],
      "simpleBackoffActor",
      3 seconds,  // then 6s, 12s, 24s
      30 seconds,
      0.2
    )
  )

//  val simpleBackoffSupervisor = system.actorOf(simpleSupervisorProps, "simpleSupervisor")
//  simpleBackoffSupervisor ! ReadFile

/*
  simpleSupervisor
    - child called simpleBackoffActor(props of type FileBasedPersistentActor)
    - supervision strategy is the default one(restarting on everything)
      - first attempt after 3 seconds
      - next attempt is 2x the previous attempt
 */

  val stopSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[FileBasedPersistentActor],
      "stopBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    ).withSupervisorStrategy(
      OneForOneStrategy() {
        case _ => Stop
      }
    )
  )

//  val simpleStopSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")
//  simpleStopSupervisor ! ReadFile

  class EagerFBPActor extends FileBasedPersistentActor {
    override def preStart(): Unit = {
      log.info("Eager actor starting...")
      // will result in error... so wil throw ActorInitializationException
      dataSource = Source.fromFile(new File("src/main/resources/testfiles/important_data.txt"))
    }
  }

  // val eagerActor = system.actorOf(Props[EagerFBPActor], "eagerActor")
  /**
    * Default Behaviour: when ActorInitializationException is thrown -> STOP
    */

  val repeatedSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[EagerFBPActor],
      "eagerActor",
      1 second,
      30 seconds,
      0.1
    )
  )

  val repeatedSupervisor = system.actorOf(repeatedSupervisorProps, "eagerSupervisor")

  /*
    eagerSupervisor
      - child eagerActor
        - will die on start with ActorInitializationException
        - trigger the supervision strategy in eagerSupervisor => STOP eagerActor
      - backoff will kick on after 1 second, 2s, 4s, 8s, 16s

      when running eagerActor, run this app and rename file `important` to `important_data`
      with this... eagerActor will start normally after failing sometime
   */


}
