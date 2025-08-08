package forex

package object services {
  type RatesService[F[_]] = rates.Algebra[F]
  type CacheService[F[_]] = cache.Algebra[F]

  final val RatesService = rates.Service
  final val CacheService = cache.Service
}
