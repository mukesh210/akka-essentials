package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorsIntro extends App {
  // part1 - actor system
  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  // part2 - create actors
  // word count actor

  class WordCountActor extends Actor {
    // internal data
    var totalWords = 0

    // behavior
    override def receive: Receive = {
      case message: String =>
        println(s"[word counter] I have received:: ${message}")
        totalWords += message.split(" ").length
      case msg => println(s"[word counter] I can't understand ${msg.toString}")
    }
  }

  // part3 - instantiate our actor
  val wordCounter: ActorRef = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor], "anotherWordCounter")
  // part4 - communicate with our actor (async)
  wordCounter ! "I am learning Akka and it's pretty damn cool"
  anotherWordCounter! "A different message"

  // actors are fully encapsulated, can only communicate via sending messages
  // new WordCountActor

  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi... my name os ${name}")
      case _ =>
    }
  }
  // way of creating Actor with constructor argument
  // legal but discouraged
  val personActor = actorSystem.actorOf(Props(new Person("Bob")))

  personActor ! "hi"

  // best practice - create companion object and a method to return props
  // and it this method to create object
  object Person {
    def props(name: String) = Props(new Person(name))
  }

  val personActor1 = actorSystem.actorOf(Person.props("Bobby"))
  personActor1 ! "hi"
}
