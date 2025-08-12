package forex.clients.oneframe.interpreter

import cats.effect.Concurrent
import cats.syntax.all._
import forex.clients.oneframe.Protocol.{ OneFrameApiError, OneFrameRatesResponse }
import forex.config.OneFrameConfig
import forex.domain.rates.Rate
import forex.clients.oneframe.{ Algebra, RequestBuilder }
import forex.clients.oneframe.errors.OneFrameError
import io.circe.parser.decode
import org.http4s.client.Client
import org.slf4j.LoggerFactory

class HttpClient[F[_]: Concurrent](client: Client[F], config: OneFrameConfig, token: String) extends Algebra[F] {

  import RequestBuilder._

  private val logger = LoggerFactory.getLogger(getClass)

  override def getRates(pairs: List[Rate.Pair]): F[OneFrameError Either OneFrameRatesResponse] = {
    val request = buildGetRatesRequest[F](pairs, config, token)

    logger.error(s"Request to OneFrameAPI: Get Rate")
    client.run(request).use { response =>
      response.bodyText.compile.string.attempt.map {
        case Left(t) =>
          logger.error(s"Failed to read response body: ${t.getMessage}", t)
          Left(OneFrameError.fromThrowable(t))
        case Right(body) if response.status.isSuccess =>
          parseSuccessBody(body)
        case Right(body) =>
          logger.error(s"Error response from OneFrame - status:${response.status}, body:$body")
          Left(OneFrameError.HttpError(response.status.code, body))
      }
    }
  }

  private def parseSuccessBody(body: String): Either[OneFrameError, OneFrameRatesResponse] =
    decode[OneFrameRatesResponse](body)
      .leftFlatMap { _ =>
        decode[OneFrameApiError](body)
          .leftMap(_ => OneFrameError.decodedFailed(body))
          .flatMap(apiError => {
            logger.error(s"Received error from OneFrameAPI: $apiError")
            Left(OneFrameError.fromApiError(apiError.error))
          })
      }
      .flatMap(ensureNonEmpty)

  private def ensureNonEmpty(r: OneFrameRatesResponse): Either[OneFrameError, OneFrameRatesResponse] =
    if (r.nonEmpty) {
      logger.info(s"Received response from OneFrameAPI: ${r.size} rates")
      Right(r)
    } else {
      logger.warn("Received response from OneFrameAPI: Empty Rate")
      Left(OneFrameError.noRateFound)
    }

}

object HttpClient {
  def apply[F[_]: Concurrent](client: Client[F], config: OneFrameConfig, token: String): Algebra[F] =
    new HttpClient[F](client, config, token)
}
