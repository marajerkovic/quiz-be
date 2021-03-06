package com.example

//#user-registry-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.collection.immutable
import scala.util.{Random, Try}

//#user-case-classes
final case class FunFact(fact: String, owner: String)
final case class FunFactQuestion(fact: String, options: List[String], answer: String)
final case class NumberOfFacts(value: Int)
final case class AllFacts(facts: List[FunFact])
case class FactOk()
//#user-case-classes

object Registry {
  // actor protocol
  sealed trait Command
  final case class GetNumberOfFacts(replyTo: ActorRef[NumberOfFacts]) extends Command
  final case class GetAllFacts(replyTo: ActorRef[AllFacts]) extends Command
  final case class CreateFact(fact: FunFact, replyTo: ActorRef[FactOk]) extends Command
  final case class GetQuestion(replyTo: ActorRef[FunFactQuestion]) extends Command
  final case class ResetQuestions(replyTo: ActorRef[FactOk]) extends Command
  final case class ResetFacts(replyTo: ActorRef[FactOk]) extends Command

  val random = new Random()

  def apply(): Behavior[Command] = registry(Set.empty, Set.empty, Set.empty)

  val dummyFunFact = FunFactQuestion("No more questions", List("1","2","3","4"), "1")

  private def registry(users: Set[String], allFacts: Set[FunFact], pendingFacts: Set[FunFact]): Behavior[Command] =
    Behaviors.receiveMessage {
      case CreateFact(fact: FunFact, replyTo) =>
        replyTo ! FactOk()
        val normalizedFact = fact.copy(owner = fact.owner.trim.toUpperCase())
        registry(users + normalizedFact.owner, allFacts + normalizedFact, pendingFacts + normalizedFact)
      case GetNumberOfFacts(replyTo) =>
        replyTo ! NumberOfFacts(pendingFacts.size)
        Behaviors.same
      case GetAllFacts(replyTo) =>
        replyTo ! AllFacts(allFacts.toList)
        Behaviors.same
      case GetQuestion(replyTo) => Try {
        val index = random.nextInt(pendingFacts.size)
        val question: FunFact = pendingFacts.toList(index)
        var otherUsers: List[String] = (users - question.owner).toList
        val opt1: String = otherUsers.apply(random.nextInt(otherUsers.size))
        otherUsers = otherUsers.filterNot(_.equals(opt1))
        val opt2: String = otherUsers.apply(random.nextInt(otherUsers.size))
        otherUsers = otherUsers.filterNot(_.equals(opt2))
        val opt3: String = otherUsers.apply(random.nextInt(otherUsers.size))
        replyTo ! FunFactQuestion(question.fact, Random.shuffle(List(opt1, opt2, opt3, question.owner)), question.owner)
        registry(users, allFacts, pendingFacts - question)
      }.getOrElse({
        replyTo ! dummyFunFact
        Behaviors.same
      })
      case ResetQuestions(replyTo) =>
        replyTo ! FactOk()
        registry(users, allFacts, allFacts)
      case ResetFacts(replyTo) =>
        replyTo ! FactOk()
        registry(Set.empty, Set.empty, Set.empty)
    }
}
