package forex.integrations.oneframe.interpreter

import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.all._
import forex.config.OneFrameConfig
import forex.domain.rates.Rate
import forex.integrations.oneframe.Protocol.{ GetRateResponse, OneFrameArrayResponse }
import forex.integrations.oneframe.{ Algebra, UriBuilder }
import forex.integrations.oneframe.errors.OneFrameError
import org.http4s.Status.Ok
import org.http4s.client.Client
import org.http4s.circe.CirceEntityCodec._

class HttpClient[F[_]: Concurrent](
    client: Client[F],
    config: OneFrameConfig,
    token: String
) extends Algebra[F] {

  import UriBuilder._

  override def getRates(pairs: List[Rate.Pair]): F[OneFrameError Either GetRateResponse] = {
    val request = buildGetRatesRequest[F](pairs, config, token)

    EitherT(client.run(request).use(handleResponse))
      .leftMap(OneFrameError.fromThrowable)
      .value
  }

  private def handleResponse(response: org.http4s.Response[F]): F[OneFrameError Either GetRateResponse] =
    response.status match {
      case Ok =>
        EitherT(response.as[OneFrameArrayResponse].attempt)
          .leftMap(e => OneFrameError.DecodingError(e.getMessage))
          .map(arrayResponse => GetRateResponse(arrayResponse))
          .subflatMap(validateResponse)
          .value
      case _ =>
        handleErrorResponse(response).map(_.asLeft[GetRateResponse])
    }

  private def validateResponse(response: GetRateResponse): Either[OneFrameError, GetRateResponse] =
    response.rates.headOption match {
      case Some(_) => response.asRight[OneFrameError]
      case None    =>
        OneFrameError
          .OneFrameLookupFailed(
            s"No rate found for pair ${response.rates.headOption.map(r => s"${r.from}${r.to}").getOrElse("unknown")}"
          )
          .asLeft[GetRateResponse]
    }

  private def handleErrorResponse(response: org.http4s.Response[F]): F[OneFrameError] =
    response.as[String].attempt.map {
      case Right(body) => OneFrameError.HttpError(response.status.code, body)
      case Left(e) => OneFrameError.HttpError(response.status.code, s"Failed to read response body: ${e.getMessage}")
    }
}

object HttpClient {
  def apply[F[_]: Concurrent](client: Client[F], config: OneFrameConfig, token: String): Algebra[F] =
    new HttpClient[F](client, config, token)
}
