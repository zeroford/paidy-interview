package forex

import forex.domain.rates.PivotRate

package object services {
  type RatesService[F[_]] = rates.Algebra[F]
  type CacheService[F[_]] = cache.Algebra[F]

  type PivotPair = (PivotRate, PivotRate)

  final val RatesService = rates.Service
  final val CacheService = cache.Service
}
