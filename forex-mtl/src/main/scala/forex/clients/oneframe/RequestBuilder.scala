package forex.clients.oneframe

import forex.domain.rates.Rate
import org.http4s._
import org.typelevel.ci.CIStringSyntax

final case class RequestBuilder(host: String, port: Int, token: String) {

  private val baseUri: Uri = Uri(
    scheme = Some(Uri.Scheme.http),
    authority = Some(Uri.Authority(host = Uri.RegName(host), port = Some(port)))
  )
  private val header: Headers = Headers(Header.Raw(ci"token", token))

  def getRatesRequest[F[_]](pairs: List[Rate.Pair]): Request[F] = {
    val query      = Query.fromPairs(pairs.map(p => "pair" -> s"${p.from}${p.to}"): _*)
    val requestUrl = (baseUri / "rates").copy(query = query)
    Request[F]().withMethod(Method.GET).withUri(requestUrl).withHeaders(header)
  }
}
