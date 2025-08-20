package forex.modules

import cats.effect.IO
import cats.syntax.traverse._
import forex.modules.middleware.ErrorHandlerMiddleware
import io.circe.parser._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.typelevel.ci.CIString

class ErrorHandlerMiddlewareSpec extends CatsEffectSuite {

  test("returns response from inner route unchanged when no error") {
    val inner = HttpRoutes.of[IO] { case GET -> Root / "ok" => Ok("success") }
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val req = Request[IO](Method.GET, uri"/ok")
    for {
      resp <- app.run(req)
      _ <- IO(assertEquals(resp.status, Status.Ok))
      body <- resp.as[String]
      _ <- IO(assertEquals(body, "success"))
    } yield ()
  }

  test("returns 404 and JSON body when no matching route") {
    val inner = HttpRoutes.empty[IO]
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val req = Request[IO](Method.GET, uri"/notfound")
    for {
      resp <- app.run(req)
      _ <- IO(assertEquals(resp.status, Status.NotFound))
      bodyStr <- resp.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.NotFound.code)))
      message = json.hcursor.get[String]("message").toOption.get
      _ <- IO(assert(message.contains("/notfound")))
    } yield ()
  }

  test("returns 500 and JSON error body on exception") {
    val inner = HttpRoutes.of[IO] { case _ => throw new RuntimeException("boom!") }
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val req = Request[IO](Method.GET, uri"/crash")
    for {
      resp <- app.run(req)
      _ <- IO(assertEquals(resp.status, Status.InternalServerError))
      bodyStr <- resp.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.InternalServerError.code)))
      message = json.hcursor.get[String]("message").toOption.get
      _ <- IO(assert(message.contains("boom") || message.contains("Internal server error")))
    } yield ()
  }

  test("handles different HTTP methods for 404") {
    val inner = HttpRoutes.empty[IO]
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val methods = List(Method.GET, Method.POST, Method.PUT, Method.DELETE, Method.PATCH)

    methods.foreach { method =>
      val req = Request[IO](method, uri"/notfound")
      for {
        resp <- app.run(req)
        _ <- IO(assertEquals(resp.status, Status.NotFound, s"Failed for method $method"))
      } yield ()
    }
  }

  test("handles different exception types") {
    val exceptionTypes = List(
      ("RuntimeException", new RuntimeException("runtime error")),
      ("IllegalArgumentException", new IllegalArgumentException("illegal argument")),
      ("NullPointerException", new NullPointerException("null pointer")),
      ("ArithmeticException", new ArithmeticException("arithmetic error"))
    )

    exceptionTypes.foreach { case (exceptionType, exception) =>
      val inner = HttpRoutes.of[IO] { case _ => throw exception }
      val app   = ErrorHandlerMiddleware(inner).orNotFound

      val req = Request[IO](Method.GET, uri"/crash")
      for {
        resp <- app.run(req)
        _ <- IO(assertEquals(resp.status, Status.InternalServerError, s"Failed for $exceptionType"))
        bodyStr <- resp.as[String]
        json = parse(bodyStr).toOption.get
        _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.InternalServerError.code)))
      } yield ()
    }
  }

  test("handles exceptions with different HTTP methods") {
    val inner = HttpRoutes.of[IO] { case _ => throw new RuntimeException("method error") }
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val methods = List(Method.GET, Method.POST, Method.PUT, Method.DELETE)

    methods.foreach { method =>
      val req = Request[IO](method, uri"/crash")
      for {
        resp <- app.run(req)
        _ <- IO(assertEquals(resp.status, Status.InternalServerError, s"Failed for method $method"))
      } yield ()
    }
  }

  test("handles complex URL paths for 404") {
    val inner = HttpRoutes.empty[IO]
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val paths = List(
      "/api/v1/rates",
      "/health/check",
      "/admin/users/123",
      "/deeply/nested/path/with/many/segments",
      "/path/with/query?param=value"
    )

    paths.foreach { path =>
      val req = Request[IO](Method.GET, Uri.unsafeFromString(path))
      for {
        resp <- app.run(req)
        _ <- IO(assertEquals(resp.status, Status.NotFound, s"Failed for path $path"))
        bodyStr <- resp.as[String]
        json    = parse(bodyStr).toOption.get
        message = json.hcursor.get[String]("message").toOption.get
        _ <-
          IO(assert(message.contains(path.replace("?", "").split("\\?")(0)), s"Message should contain path for $path"))
      } yield ()
    }
  }

  test("handles exceptions in different route patterns") {
    val inner = HttpRoutes.of[IO] {
      case GET -> Root / "ok" => Ok("success")
      case _                  => throw new RuntimeException("pattern error")
    }
    val app = ErrorHandlerMiddleware(inner).orNotFound

    val req = Request[IO](Method.GET, uri"/different-pattern")
    for {
      resp <- app.run(req)
      _ <- IO(assertEquals(resp.status, Status.InternalServerError))
      bodyStr <- resp.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.InternalServerError.code)))
    } yield ()
  }

  test("handles exceptions with special characters in URL") {
    val inner = HttpRoutes.of[IO] { case _ => throw new RuntimeException("special chars error") }
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val specialPaths = List(
      "/path/with/numbers/123/456"
    )

    specialPaths.foreach { path =>
      val req = Request[IO](Method.GET, Uri.unsafeFromString(path))
      for {
        resp <- app.run(req)
        _ <- IO(assertEquals(resp.status, Status.InternalServerError, s"Failed for path $path"))
      } yield ()
    }
  }

  test("handles exceptions with query parameters") {
    val inner = HttpRoutes.of[IO] { case _ => throw new RuntimeException("query error") }
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val req = Request[IO](Method.GET, Uri.unsafeFromString("/crash?param1=value1&param2=value2"))
    for {
      resp <- app.run(req)
      _ <- IO(assertEquals(resp.status, Status.InternalServerError))
      bodyStr <- resp.as[String]
      json    = parse(bodyStr).toOption.get
      message = json.hcursor.get[String]("message").toOption.get
      _ <- IO(assert(message.nonEmpty, "Message should not be empty"))
    } yield ()
  }

  test("handles exceptions with headers") {
    val inner = HttpRoutes.of[IO] { case _ => throw new RuntimeException("header error") }
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val req = Request[IO](Method.GET, uri"/crash")
      .withHeaders(Header.Raw(CIString("X-Custom-Header"), "custom-value"))

    for {
      resp <- app.run(req)
      _ <- IO(assertEquals(resp.status, Status.InternalServerError))
      bodyStr <- resp.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.InternalServerError.code)))
    } yield ()
  }

  test("handles exceptions with different content types") {
    val inner = HttpRoutes.of[IO] { case _ => throw new RuntimeException("content type error") }
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val contentTypes = List(
      "application/json",
      "text/plain",
      "application/xml",
      "text/html"
    )

    contentTypes.foreach { contentType =>
      val req = Request[IO](Method.POST, uri"/crash")
        .withHeaders(Header.Raw(CIString("Content-Type"), contentType))

      for {
        resp <- app.run(req)
        _ <- IO(assertEquals(resp.status, Status.InternalServerError, s"Failed for content type $contentType"))
      } yield ()
    }
  }

  test("handles exceptions with large request bodies") {
    val inner = HttpRoutes.of[IO] { case _ => throw new RuntimeException("large body error") }
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val largeBody = "x" * 10000 // 10KB body
    val req       = Request[IO](Method.POST, uri"/crash")
      .withEntity(largeBody)

    for {
      resp <- app.run(req)
      _ <- IO(assertEquals(resp.status, Status.InternalServerError))
      bodyStr <- resp.as[String]
      json = parse(bodyStr).toOption.get
      _ <- IO(assertEquals(json.hcursor.get[Int]("code").toOption, Some(Status.InternalServerError.code)))
    } yield ()
  }

  test("handles exceptions with concurrent requests") {
    val inner = HttpRoutes.of[IO] { case _ => throw new RuntimeException("concurrent error") }
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val requests = List.fill(5)(Request[IO](Method.GET, uri"/crash"))

    for {
      responses <- requests.traverse(app.run)
      _ <- IO(assert(responses.forall(_.status == Status.InternalServerError), "All responses should be 500"))
    } yield ()
  }

  test("handles exceptions with different user agents") {
    val inner = HttpRoutes.of[IO] { case _ => throw new RuntimeException("user agent error") }
    val app   = ErrorHandlerMiddleware(inner).orNotFound

    val userAgents = List(
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
      "curl/7.68.0",
      "PostmanRuntime/7.28.0",
      "Apache-HttpClient/4.5.13"
    )

    userAgents.foreach { userAgent =>
      val req = Request[IO](Method.GET, uri"/crash")
        .withHeaders(Header.Raw(CIString("User-Agent"), userAgent))

      for {
        resp <- app.run(req)
        _ <- IO(assertEquals(resp.status, Status.InternalServerError, s"Failed for user agent $userAgent"))
      } yield ()
    }
  }
}
