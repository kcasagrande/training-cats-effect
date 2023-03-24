package example.producerconsumer

import cats.effect._
import cats.effect.std.Console
import cats.syntax.all._

import scala.collection.immutable.Queue

object ProducerConsumer extends IOApp {

  private def producer[F[_] : Sync : Console](queueRef: Ref[F, Queue[Int]], counter: Int): F[Unit] =
    for {
      _ <- if(counter % 10000 == 0) {
        Console[F].println(s"Produced $counter items")
      } else {
        Sync[F].unit
      }
      _ <- queueRef.getAndUpdate(_.enqueue(counter + 1))
      _ <- producer(queueRef, counter + 1)
    } yield ()

  private def consumer[F[_]: Sync : Console](queueRef: Ref[F, Queue[Int]]): F[Unit] =
    for {
      iO <- queueRef.modify { queue =>
        queue.dequeueOption.fold((queue, Option.empty[Int])) {
          case (i, queue) => (queue, Option(i))
        }
      }
      _ <- if(iO.exists(_ % 10000 == 0)) {
        Console[F].println(s"Consumed ${iO.get} items")
      } else {
        Sync[F].unit
      }
      _ <- consumer(queueRef)
    } yield ()

  override def run(args: List[String]): IO[ExitCode] =
    for {
      queueRef <- Ref.of[IO, Queue[Int]](Queue.empty[Int])
      result <- (consumer(queueRef), producer(queueRef, 0))
        .parMapN((_, _) => ExitCode.Success)
        .handleErrorWith { throwable =>
          Console[IO].errorln(s"Error caught: ${throwable.getMessage}").as(ExitCode.Error)
        }
    } yield result

}
