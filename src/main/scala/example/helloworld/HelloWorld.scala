package example.helloworld

import cats.effect.{IO, IOApp}

object HelloWorld extends IOApp.Simple {
  val run: IO[Unit] = IO.println("Hello, world!")
}