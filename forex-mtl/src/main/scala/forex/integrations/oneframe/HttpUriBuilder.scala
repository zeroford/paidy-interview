package forex.integrations.oneframe

import forex.config.OneFrameConfig
import forex.domain.rates.Rate
import org.http4s.{ Header, Headers, Method, Request, Uri }
import org.typelevel.ci.CIString

object HttpUriBuilder {

  private def authHeader(token: String): Header.Raw =
    Header.Raw(CIString("token"), token)

  private def buildBaseUri(config: OneFrameConfig): Uri =
    Uri(
      scheme = Some(Uri.Scheme.http),
      authority = Some(
        Uri.Authority(
          host = Uri.RegName(config.host),
          port = Some(config.port)
        )
      )
    )

  def buildGetRatesRequest[F[_]](pairs: List[Rate.Pair], config: OneFrameConfig): Request[F] = {
    val pairParams = pairs.map(pair => s"${pair.from}${pair.to}")
    val baseUri    = buildBaseUri(config).withPath(Uri.Path.unsafeFromString("/rates"))

    val uriWithParams = pairParams.foldLeft(baseUri) { (uri, param) =>
      uri.withQueryParam("pair", param)
    }

    Request[F](
      method = Method.GET,
      uri = uriWithParams,
      headers = Headers(authHeader(config.token))
    )
  }
}
