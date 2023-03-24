package example

import cats.effect.{IO, IOApp}

import scala.concurrent.duration._

object FizzBuzz extends IOApp.Simple {
  val run = for {
    counter <- IO.ref(0)
    wait = IO.sleep(1.second)
    poll = wait *> counter.get
    _ <- poll.flatMap(IO.println).foreverM.start
    _ <- poll.map(_ % 3 == 0).ifM(IO.println("Fizz"), IO.unit).foreverM.start
    _ <- poll.map(_ % 5 == 0).ifM(IO.println("Buzz"), IO.unit).foreverM.start
    _ <- (wait *> counter.update(_ + 1)).foreverM.void
  } yield ()
}
