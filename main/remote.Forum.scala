package remote

import data._
import scalaj.http._
import spray.json._
import DefaultJsonProtocol._

object Forum {
  def dump: String = download(config.dump)

  // download field names together with ids from the forum
  lazy val fieldNames: data.FieldNames =
    download(config.listFields).parseJson.convertTo[data.FieldNames]

  def download(url: String): String = {
    val response: HttpResponse[String] =
      Http(config.appendApiKey(url)).asString

    if (response.isError)
      sys error response.toString
    else
      response.body
  }

  implicit class HttpOps(http: HttpRequest) {
    // must be called last, because HttpRequest.postXYZ set method to POST.
    def doPut: HttpRequest = http.method("PUT")

    def asJson: HttpRequest = http.header("Content-type", "application/json")
  }

  def putJson(url: String, json: JsValue): HttpRequest =
    Http(config.appendApiKey(url)).postData(json.compactPrint).asJson.doPut

  def setUserFields(fields: Seq[data.Student.Field]): Unit = {
    val json = Map(config.dataKey -> fields.map(_.toJson)).toJson
    expectTruth(putJson(config.setUserFields, json))
  }

  def setUserField(userid: Int, key: String, value: String): Unit =
    expectTruth(
      Http(config.setUserField).postForm(
        Seq(
          config.apiKey,
          (config.userid    , userid.toString),
          (config.userfield , key),
          (config.value     , value)
        )
      ).doPut
    )

  def expectTruth(request: HttpRequest): Unit = {
    val response = request.asString
    if (response.isError)
      sys error response.toString
    else {
      import DefaultJsonProtocol._
      val map = response.body.parseJson.convertTo[JsValue].asJsObject
      map.fields.get(config.success) match {
        case Some(JsBoolean(true)) => ()
        case _                     => sys error map.prettyPrint
      }
    }
  }

  def getUsers(): Users = Users.fromJson(dump)
}
