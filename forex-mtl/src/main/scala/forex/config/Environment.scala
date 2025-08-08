package forex.config

sealed trait Environment
object Environment {
  case object Dev extends Environment
  case object Test extends Environment
}
