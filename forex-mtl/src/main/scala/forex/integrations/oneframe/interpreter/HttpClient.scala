package forex.integrations.oneframe.interpreter

import cats.effect.Concurrent
import cats.syntax.all._
import forex.config.OneFrameConfig
import forex.domain.rates.Rate
import forex.integrations.oneframe.Protocol.GetRateResponse
import forex.integrations.oneframe.{ Algebra, HttpUriBuilder }
import forex.integrations.oneframe.errors.OneFrameError
import org.http4s.Status.Ok
import org.http4s.client.Client
import org.http4s.circe.CirceEntityCodec._

class HttpClient[F[_]: Concurrent](client: Client[F], config: OneFrameConfig) extends Algebra[F] {

  import HttpUriBuilder._

  override def getRate(pair: Rate.Pair): F[OneFrameError Either GetRateResponse] = {
    val request = buildGetRateRequest[F](pair, config)
    client
      .run(request)
      .use { response =>
        response.status match {
          case Ok =>
            response.as[GetRateResponse].attempt.map {
              case Right(GetRateResponse(rates)) =>
                rates.headOption match {
                  case Some(_) => GetRateResponse(rates).asRight
                  case None    =>
                    OneFrameError.OneFrameLookupFailed(s"No rate found for pair ${pair.from}${pair.to}").asLeft
                }
              case Left(e) => OneFrameError.DecodingError(e.getMessage).asLeft
            }
          case _ => handleErrorResponse(response).map(_.asLeft)
        }
      }
      .handleErrorWith(OneFrameError.fromThrowable(_).asLeft[GetRateResponse].pure[F])
  }

  private def handleErrorResponse(response: org.http4s.Response[F]): F[OneFrameError] =
    response.as[String].attempt.map {
      case Right(body) => OneFrameError.HttpError(response.status.code, body)
      case Left(e) => OneFrameError.HttpError(response.status.code, s"Failed to read response body: ${e.getMessage}")
    }

}

object HttpClient {
  def apply[F[_]: Concurrent](client: Client[F], config: OneFrameConfig): Algebra[F] =
    new HttpClient[F](client, config)
}
