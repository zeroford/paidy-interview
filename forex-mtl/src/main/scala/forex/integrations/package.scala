package forex

package object integrations {
  type OneFrameClient[F[_]] = oneframe.Algebra[F]

  final val OneFrameHttpClient = oneframe.interpreter.HttpClient
  final val OneFrameDummyClient = oneframe.interpreter.DummyClient
}
