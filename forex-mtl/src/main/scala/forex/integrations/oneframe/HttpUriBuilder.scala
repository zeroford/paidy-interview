package forex.integrations.oneframe

import forex.config.OneFrameConfig
import forex.domain.rates.Rate
import org.http4s.{ Header, Headers, Method, Request, Uri }
import org.typelevel.ci.CIString

object HttpUriBuilder {

  private def authHeader(token: String): Header.Raw =
    Header.Raw(CIString("token"), token)

  def buildGetRatesRequest[F[_]](pairs: List[Rate.Pair], config: OneFrameConfig): Request[F] = {
    val pairParams  = pairs.map(pair => s"${pair.from}${pair.to}")
    val queryString = pairParams.map(param => s"pair=$param").mkString("&")
    val uriString   = s"http://${config.host}:${config.port}/rates?$queryString"
    val uri         = Uri.unsafeFromString(uriString)

    Request[F](
      method = Method.GET,
      uri = uri,
      headers = Headers(authHeader(config.token))
    )
  }
}
