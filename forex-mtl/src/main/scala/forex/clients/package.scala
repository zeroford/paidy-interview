package forex

package object clients {
  type OneFrameClient[F[_]] = oneframe.Algebra[F]

  final val OneFrameClient = oneframe.Interpreters
}
