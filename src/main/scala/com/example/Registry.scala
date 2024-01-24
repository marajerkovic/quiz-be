package com.example

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.collection.immutable
import scala.util.{Random, Try}

final case class FunFact(fact: String, owner: String)
final case class FunFactQuestion(fact: String, options: List[String], answer: String)
final case class NumberOfFacts(value: Int)
final case class AllFacts(facts: List[FunFact])

object Registry {
  sealed trait Command
  final case class GetNumberOfFacts(replyTo: ActorRef[NumberOfFacts]) extends Command
  final case class GetAllFacts(replyTo: ActorRef[AllFacts]) extends Command
  final case class CreateFact(fact: FunFact, replyTo: ActorRef[FactOk]) extends Command
  final case class CreateBulkFacts(fact: AllFacts, replyTo: ActorRef[FactOk]) extends Command
  final case class GetQuestion(replyTo: ActorRef[FunFactQuestion]) extends Command
  final case class ResetQuestions(replyTo: ActorRef[FactOk]) extends Command
  final case class ResetFacts(replyTo: ActorRef[FactOk]) extends Command
  final case class FactOk() extends Command

  val random = new Random()

  def apply(): Behavior[Command] = registry(Set.empty, Set.empty, Set.empty)

  val dummyFunFact = FunFactQuestion("No more questions", List("1","2","3","4"), "1")

  private def registry(users: Set[String], allFacts: Set[FunFact], pendingFacts: Set[FunFact]): Behavior[Command] =
    Behaviors.receive({

      case (ctx, CreateFact(fact: FunFact, replyTo)) =>
        ctx.log.info("Received CreateFact with {}", fact)
        replyTo ! FactOk()
        val normalizedFact = fact.copy(owner = fact.owner.trim.toUpperCase())
        registry(users + normalizedFact.owner, allFacts + normalizedFact, pendingFacts + normalizedFact)

      case (ctx, CreateBulkFacts(facts: AllFacts, replyTo)) =>
        ctx.log.info("Received CreateBulkFacts with {}", facts)
        replyTo ! FactOk()
        ctx.self ! ResetFacts(ctx.self)
        facts.facts.foreach(f => ctx.self ! CreateFact(f, ctx.self))
        Behaviors.same

      case (ctx, GetNumberOfFacts(replyTo)) =>
        ctx.log.info("Received GetNumberOfFacts")
        replyTo ! NumberOfFacts(pendingFacts.size)
        Behaviors.same

      case (ctx, GetAllFacts(replyTo)) =>
        ctx.log.info("Received GetAllFacts")
        replyTo ! AllFacts(allFacts.toList)
        Behaviors.same

      case (ctx, GetQuestion(replyTo)) => Try {
        ctx.log.info("Received GetQuestion")
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

      case (ctx, ResetQuestions(replyTo)) =>
        ctx.log.info("Received ResetQuestions")
        replyTo ! FactOk()
        registry(users, allFacts, allFacts)

      case (ctx, ResetFacts(replyTo)) =>
        ctx.log.info("Received ResetFacts")
        replyTo ! FactOk()
        registry(Set.empty, Set.empty, Set.empty)

      case (ctx, FactOk()) =>
        ctx.log.info("Received FactOk")
        Behaviors.same

    })

}
