package com.example

//#json-formats
import spray.json.{DefaultJsonProtocol, JsArray, JsValue, JsonFormat, RootJsonFormat}

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val funFactQuestion = jsonFormat3(FunFactQuestion)
  implicit val numberOfFacts = jsonFormat1(NumberOfFacts)
  implicit val funFact = jsonFormat2(FunFact)
  implicit val allFacts = jsonFormat1(AllFacts)
  implicit val factOk = jsonFormat0(FactOk)

}
