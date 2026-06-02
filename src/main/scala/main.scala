import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink, Source}
import akka.util.ByteString

import scala.concurrent.ExecutionContext
import scala.io.StdIn

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("VideoStreamingSystem")
  implicit val executionContext: ExecutionContext = system.dispatcher

  // Create a broadcast hub for distributing video frames to all viewers
  val (videoSink, videoSource) = MergeHub
    .source[ByteString](256)
    .toMat(BroadcastHub.sink[ByteString](256))(Keep.both)
    .run()

  // WebSocket flow for broadcaster (camera source)
  def broadcasterFlow: Flow[Message, Message, Any] = {
    val sink = Flow[Message]
      .collect {
        case BinaryMessage.Strict(data) => data
        case TextMessage.Strict(text) => ByteString(text)
      }
      .to(videoSink)

    Flow.fromSinkAndSourceCoupled(sink, Source.maybe[Message])
  }

  // WebSocket flow for viewers
  def viewerFlow: Flow[Message, Message, Any] = {
    val messageSource = videoSource.map(data => BinaryMessage.Strict(data): Message)
    Flow.fromSinkAndSource(Sink.ignore, messageSource)
  }

  // CORS headers for cross-origin requests
  val corsHeaders = List(
    headers.`Access-Control-Allow-Origin`.*,
    headers.`Access-Control-Allow-Methods`(HttpMethods.GET, HttpMethods.POST, HttpMethods.OPTIONS),
    headers.`Access-Control-Allow-Headers`("Content-Type", "Authorization")
  )

  val route = {
    respondWithHeaders(corsHeaders) {
      concat(
        // Health check endpoint
        path("health") {
          get {
            complete(HttpEntity(ContentTypes.`application/json`, """{"status": "ok"}"""))
          }
        },
        // WebSocket endpoint for broadcaster (camera)
        path("ws" / "broadcast") {
          handleWebSocketMessages(broadcasterFlow)
        },
        // WebSocket endpoint for viewers
        path("ws" / "view") {
          handleWebSocketMessages(viewerFlow)
        },
        // Serve static frontend files
        pathEndOrSingleSlash {
          getFromResource("static/index.html")
        },
        path("broadcaster.html") {
          getFromResource("static/broadcaster.html")
        },
        pathPrefix("static") {
          getFromResourceDirectory("static")
        }
      )
    }
  }

  val bindingFuture = Http().newServerAt("0.0.0.0", 8080).bind(route)

  println(s"Server online at http://localhost:8080/")
  println(s"Broadcaster page: http://localhost:8080/broadcaster.html")
  println(s"Viewer page: http://localhost:8080/")
  println(s"Press ENTER to stop...")

  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
