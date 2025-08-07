package forex

package object integrations {
  type OneFrameClient[F[_]] = oneframe.Algebra[F]

  final val OneFrameInterpreters  = oneframe.Interpreters
}
