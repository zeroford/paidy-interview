package forex.integrations.oneframe

import forex.config.OneFrameConfig
import forex.domain.rates.Rate
import org.http4s.{ Header, Headers, Method, Request, Uri }
import org.typelevel.ci.CIString

object HttpUriBuilder {

  def authHeader(token: String): Header.Raw =
    Header.Raw(CIString("token"), token)

  def buildBaseUri(config: OneFrameConfig): Uri =
    Uri(
      scheme = Some(Uri.Scheme.http),
      authority = Some(
        Uri.Authority(
          host = Uri.RegName(config.host),
          port = Some(config.port)
        )
      )
    )

  def buildGetRateUri(pair: Rate.Pair, config: OneFrameConfig): Uri =
    buildBaseUri(config)
      .withPath(Uri.Path.unsafeFromString("/rates"))
      .withQueryParam("pair", s"${pair.from}${pair.to}")

  def buildGetRateRequest[F[_]](pair: Rate.Pair, config: OneFrameConfig): Request[F] =
    Request[F](
      method = Method.GET,
      uri = buildGetRateUri(pair, config),
      headers = Headers(authHeader(config.token))
    )
}
