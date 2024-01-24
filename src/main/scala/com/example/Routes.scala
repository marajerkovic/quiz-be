package com.example

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import com.example.Registry._
import akka.actor.typed.ActorRef
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import com.example.Registry.{CreateFact, GetAllFacts, GetNumberOfFacts, GetQuestion}

class Routes(userRegistry: ActorRef[Registry.Command])(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getAllFacts(): Future[AllFacts] = userRegistry.ask(GetAllFacts)
  def getNumberOfFacts(): Future[NumberOfFacts] = userRegistry.ask(GetNumberOfFacts)
  def createFact(funFact: FunFact): Future[FactOk] = userRegistry.ask(CreateFact(funFact, _))
  def createBulkFacts(funFacts: AllFacts): Future[FactOk] = userRegistry.ask(CreateBulkFacts(funFacts, _))
  def getQuestion(): Future[FunFactQuestion] = userRegistry.ask(GetQuestion)
  def resetQuestions(): Future[FactOk] = userRegistry.ask(ResetQuestions)
  def resetFacts(): Future[FactOk] = userRegistry.ask(ResetFacts)

  val routes: Route =
    cors() {
      pathPrefix("facts") {
        post {
          entity(as[FunFact]) { fact =>
            onSuccess(createFact(fact)) { performed =>
              system.log.info("post /facts")
              complete(StatusCodes.Created, performed)
            }
          }
        } ~
          path("all") {
            get {
              onSuccess(getAllFacts()) { facts =>
                system.log.info("get /facts/all")
                complete(facts)
              }
            }
          } ~
          path("pending") {
            get {
              onSuccess(getNumberOfFacts()) { n =>
                system.log.info("get /facts/pending")
                complete(n)
              }
            }
          } ~
          path("bulk") {
            post {
              entity(as[AllFacts]) { facts =>
                onSuccess(createBulkFacts(facts)) { performed =>
                  system.log.info("post /facts/bulk")
                  complete(StatusCodes.Created, performed)
                }
              }
            }
          }
      } ~
        path("admin" / "questions") {
          get {
            onSuccess(getQuestion()) { q =>
              system.log.info("get /admin/questions")
              complete(q)
            }
          } ~
            delete {
              onSuccess(resetQuestions()) { performed =>
                system.log.info("delete /admin/questions")
                complete(StatusCodes.OK, performed)
              }
            }
        } ~
        path("admin" / "facts") {
            delete {
              onSuccess(resetFacts()) { performed =>
                system.log.info("delete /admin/facts")
                complete(StatusCodes.OK, performed)
              }
            }
        }
    }
}
