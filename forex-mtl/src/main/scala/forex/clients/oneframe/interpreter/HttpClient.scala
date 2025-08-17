package forex.clients.oneframe.interpreter

import cats.effect.Concurrent
import cats.syntax.all._
import io.circe.parser.decode
import org.http4s.client.Client
import org.typelevel.log4cats.Logger

import forex.clients.oneframe.{ Algebra, RequestBuilder }
import forex.clients.oneframe.Protocol.{ OneFrameApiError, OneFrameRatesResponse }
import forex.clients.oneframe.{ errors => Error }
import forex.config.OneFrameConfig
import forex.domain.error.AppError
import forex.domain.rates.Rate

final class HttpClient[F[_]: Concurrent: Logger](client: Client[F], config: OneFrameConfig, token: String)
    extends Algebra[F] {

  private val builder = RequestBuilder(config.host, config.port, token)

  override def getRates(pairs: List[Rate.Pair]): F[AppError Either OneFrameRatesResponse] = {
    val request = builder.getRatesRequest[F](pairs)
    client.run(request).use { response =>
      Logger[F].debug(s"[OneFrameAPI] Request GET rates, request:$request") >>
        response.bodyText.compile.string.attempt.flatMap {
          case Right(body) if response.status.isSuccess => parseSuccessBody(body)
          case Right(body)                              =>
            Logger[F]
              .error(s"[OneFrameAPI] Receive error, http error: ${response.status}, $body")
              .as(Error.toAppError(response.status.code, body).asLeft[OneFrameRatesResponse])
          case Left(t) =>
            Logger[F]
              .error(s"[OneFrameAPI] Receive error, read-body failed: ${t.getMessage}")
              .as(Error.toAppError(t).asLeft[OneFrameRatesResponse])
        }
    }
  }

  private def parseSuccessBody(body: String): F[AppError Either OneFrameRatesResponse] =
    decode[OneFrameRatesResponse](body) match {
      case Right(r) =>
        if (r.nonEmpty)
          Logger[F].debug(s"[OneFrameAPI] Received response: ${r.size} rates") >>
            r.asRight[AppError].pure[F]
        else
          Logger[F].warn("[OneFrameAPI] Received response: Empty rate") >>
            Error.toAppError("Empty Rate").asLeft[OneFrameRatesResponse].pure[F]
      case Left(_) =>
        decode[OneFrameApiError](body) match {
          case Right(res) =>
            Logger[F].warn(s"[OneFrameAPI] Received error: $res") >>
              Error.toAppError(res.error).asLeft[OneFrameRatesResponse].pure[F]
          case Left(e) =>
            Logger[F].error(s"[OneFrameAPI] decode failed, ${e.getMessage}; body:${body.trim}") >>
              Error.toAppError("one-frame", s"Decoding failed: ${e.getMessage}").asLeft[OneFrameRatesResponse].pure[F]
        }
    }

}

object HttpClient {
  def apply[F[_]: Concurrent: Logger](client: Client[F], config: OneFrameConfig, token: String): Algebra[F] =
    new HttpClient[F](client, config, token)
}
