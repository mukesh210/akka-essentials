package part1recap

import scala.concurrent.Future

object AdvancedRecap extends App {

  // partial functions
  val partialFunction: PartialFunction[Int, Int] = {
    case 1 => 42
    case 2 => 65
    case 5 => 999
  } // equivalent to

  val pf = (x: Int) => x match {
    case 1 => 42
    case 2 => 65
    case 5 => 999
  }

  val function: (Int => Int) = partialFunction

  val modifiedList = List(1,2,3).map{
    case 1 => 42
    case _ => 0
  }

  // lifting
  val lifted = partialFunction.lift // total function Int => Option[Int]
  lifted(2) // Some(65)
  lifted(5000)  // None

  // orElse
  val pfChain = partialFunction.orElse[Int, Int] {
    case 60 => 9000
  }

  pfChain(5) // 999
  pfChain(60) // 9000
  // pfChain(457)  // throws a matchError

  // type aliases
  type ReceiveFunction = PartialFunction[Any, Unit]

  def receive: ReceiveFunction = {
    case 1 => println("hello")
    case _ => println("confused")
  }

  // implicits
  implicit val timeout = 3000
  def setTimeout(f: () => Unit)(implicit timeout: Int) = f()

  setTimeout(() => println("timeout"))  // omit extra parameter

  // implicit conversions
  // 1. Implicit defs
  case class Person(name: String) {
    def greet = s"Hi... My name is ${name}"
  }

  implicit def fromStringToPerson(string: String): Person = Person(string)

  println("Peter".greet)

//  case class Employee(name: String) {
//    def greet = s"Employee ${name} greeting..."
//  }
//  implicit def fromStringToEmployee(str: String): Employee = Employee(str)

  // fromStringToPerson("Peter").greet  -- automatically done by compiler

  // 2. implicit classes
  implicit class Dog(name: String) {
    def bark = println("BARK...")
  }

  "Lassie".bark

  // organize implicits
  // local scope
  implicit val inverseOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
  List(1,2,3).sorted  // List(3, 2, 1)

  // define implicit in imported scope
  import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future{
    println("Hello from Future")
  }

  // companion objects of types included in the call
  object Person {
    implicit val personOrdering: Ordering[Person] = Ordering.fromLessThan(
      (a, b) => a.name.compareTo(b.name) < 0
    )
  }

  List(Person("bob"), Person("alice")).sorted
  // List(Person("alice"), Person("bob"))


  // implicit resolution
  // 1. local scope
  // 2. imported scope
  // 3. implicits defined in companion object
}
