package forex.programs.rates

import forex.domain.currency.Currency

object Protocol {

  final case class GetRatesRequest(
      from: Currency,
      to: Currency
  )

}
