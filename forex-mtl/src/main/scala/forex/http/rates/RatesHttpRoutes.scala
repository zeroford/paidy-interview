package forex.http
package rates

import cats.data.Validated.{ Invalid, Valid }
import cats.effect.Sync
import cats.syntax.all._
import forex.http.util.{ HttpErrorMapper, QueryValidator }
import forex.programs.RatesProgram
import forex.programs.rates.Protocol.GetRatesRequest
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.HttpRoutes

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, QueryParams._, Protocol._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(fromOpt) +& ToQueryParam(toOpt) =>
      QueryValidator.validate(fromOpt, toOpt) match {
        case Valid((from, to)) =>
          rates.get(GetRatesRequest(from, to)).flatMap {
            case Right(rate) => Ok(rate.asGetApiResponse)
            case Left(err)   => HttpErrorMapper.map(err)
          }
        case Invalid(err) => HttpErrorMapper.badRequest[F](err.toList)
      }
    case method -> Root => HttpErrorMapper.methodNotAllow[F](method)
  }

  val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)
}
