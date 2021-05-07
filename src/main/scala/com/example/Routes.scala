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

//#import-json-formats
//#user-routes-class
class Routes(userRegistry: ActorRef[Registry.Command])(implicit val system: ActorSystem[_]) {

  //#user-routes-class
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._
  //#import-json-formats

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getAllFacts(): Future[AllFacts] = userRegistry.ask(GetAllFacts)
  def getNumberOfFacts(): Future[NumberOfFacts] = userRegistry.ask(GetNumberOfFacts)
  def createFact(funFact: FunFact): Future[FactOk] = userRegistry.ask(CreateFact(funFact, _))
  def getQuestion(): Future[FunFactQuestion] = userRegistry.ask(GetQuestion)
  def reset(): Future[FactOk] = userRegistry.ask(Reset)

  val routes: Route =
    cors() {
      pathPrefix("facts") {
        post {
          entity(as[FunFact]) { fact =>
            onSuccess(createFact(fact)) { performed =>
              complete(StatusCodes.Created, performed)
            }
          }
        } ~
          path("all") {
            get {
              onSuccess(getAllFacts()) { facts =>
                complete(facts)
              }
            }
          } ~
          path("pending") {
            get {
              onSuccess(getNumberOfFacts()) { n =>
                complete(n)
              }
            }
          }
      } ~
        path("admin" / "questions") {
          get {
            onSuccess(getQuestion()) { q =>
              complete(q)
            }
          } ~
            delete {
              onSuccess(reset()) { performed =>
                complete(StatusCodes.OK, performed)
              }
            }
        }
    }
}
