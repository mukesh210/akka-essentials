package part1recap

import scala.concurrent.Future
import scala.util.{Failure, Success}

object MultiThreadingRecap extends App {

  // creating thread on the JVM

  val aThread = new Thread(() => println("I am running in parallel"))
  aThread.start()
  aThread.join()

  val threadHello = new Thread(() => (1 to 1000).foreach(_ => println("hello")))
  val threadGoodbye = new Thread(() => (1 to 1000).foreach(_ => println("good bye")))
  threadHello.start()
  threadGoodbye.start()

  // Threads are unpredictable: different runs produce different result!

  class BankAccount(@volatile private var amount: Int) {
    override def toString: String = "" + amount

    def withdraw(money: Int) = this.amount -= money

    def safewithdraw(money: Int) = this.synchronized{
      this.amount  -= money
    }
  }

  /*
    withdraw is not thread safe

    BA(10000)
    T1 -> withdraw 1000
    T2 -> withdraw 2000

      ... result can be 9000

      Reason: this.amount -= money is not ATOMIC
      Solution: use synchronized block or @volatile(volatile works only on primitive types)
   */

  // IPC
  // wait - notify mechanism

  // Scala Futures
  import scala.concurrent.ExecutionContext.Implicits.global
  val futures = Future {
    // computation on different thread
    42
  }

  // callbacks
  // onComplete return type is Unit
  futures.onComplete {
    case Success(value) => println(s"found ${value}")
    case Failure(exception) => println(s"ex: ${exception}")
  }

  val aProcessedFuture = futures.map(_ + 1) // Future(43)
  val aFlattenedFuture: Future[Int] = futures.flatMap { value =>
    Future(value + 2)
  } // Future(44)

  val filteredFuture = futures.filter(_ % 2 == 0) // NoSuchElementException

  // future supports for comprehension
  val aForFuture = for {
    meaning <- futures
    filteredMeaning <- filteredFuture
  } yield meaning + filteredMeaning

  // andThen, recover/recoverWith

  // Promises
}
