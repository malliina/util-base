package com.malliina.http

import cats.effect.{Async, IO}
import com.comcast.ip4s.{host, port}
import com.malliina.util.AppLogger
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text
import org.http4s.{HttpRoutes, Request, Response}

import scala.concurrent.duration.{Duration, DurationInt}

class TestEndpoints[F[_]: Async] extends Http4sDsl[F]:
  val F = Async[F]
  private val log = AppLogger(getClass)

  def initiallyFailing(sockets: WebSocketBuilder2[F]) = HttpRoutes
    .of[F]:
      case req @ GET -> Root / "ws" =>
        val conn = req.headers
          .get(ReconnectingSocket.`X-Connection`)
          .flatMap(_.head.value.toIntOption)
          .getOrElse(0)
        if conn < 3 then F.raiseError(new Exception(s"Failing conn $conn"))
        else
          val pipe: fs2.Pipe[F, WebSocketFrame, WebSocketFrame] = _.evalMap:
            case Text(message, _) => delay(Text(s"Connection $conn said: '$message'."))
            case f                => delay(f)
          sockets.build(pipe)
    .orNotFound

  private def delay[A](thunk: => A): F[A] = F.delay(thunk)

object TestServer:
  val flaky = baseServer(builder => new TestEndpoints[IO]().initiallyFailing(builder))

  def baseServer[F[_]: Async](makeApp: WebSocketBuilder2[F] => org.http4s.HttpApp[F]) =
    EmberServerBuilder
      .default[F]
      .withHost(host"0.0.0.0")
      .withPort(port"9000")
      .withHttpWebSocketApp(builder => makeApp(builder))
      .build

case class TestSystem(client: JavaHttpClient[IO], server: Server):
  def port = server.baseUri.port.getOrElse(9000)

class ClientServerTests extends munit.CatsEffectSuite:
  val flakyApp = ResourceFunFixture(
    HttpTests.client.flatMap(c => TestServer.flaky.map(s => TestSystem(c, s)))
  )

  override def munitIOTimeout: Duration = 10.seconds

  flakyApp.test("Can send and receive messages after reconnect on failure"): comp =>
    comp.client
      .socket(FullUrl.ws(s"localhost:${comp.port}", "/ws"), Map.empty, 100.millis)
      .use: socket =>
        for
          _ <- socket.events.take(1).compile.drain
          _ <- socket.sendMessage("hej")
          received <- socket.messages.take(1).compile.toList
        yield assertEquals(received.head, "Connection 3 said: 'hej'.")
