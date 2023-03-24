package example.filecopier

import cats.effect._
import cats.syntax.all._

import java.io._

object FileCopier extends IOApp {

  private def inputStream[F[_] : Sync](file: File): Resource[F, FileInputStream] =
    Resource.make {
      Sync[F].blocking(new FileInputStream(file))
    } { inputStream =>
      Sync[F].blocking(inputStream.close()).handleErrorWith(_ => Sync[F].unit)
    }

  private def outputStream[F[_] : Sync](file: File): Resource[F, FileOutputStream] =
    Resource.make {
      Sync[F].blocking(new FileOutputStream(file))
    } {
      outputStream =>
        Sync[F].blocking(outputStream.close()).handleErrorWith(_ => Sync[F].unit)
    }

  private def inputOutputStream[F[_] : Sync](inputFile: File, outputFile: File): Resource[F, (FileInputStream, FileOutputStream)] =
    for {
      in <- inputStream(inputFile)
      out <- outputStream(outputFile)
    } yield (in, out)

  private def transmit[F[_] : Sync](source: InputStream, destination: OutputStream, buffer: Array[Byte], transferredBytes: Long): F[Long] =
    for {
      readBytes <- Sync[F].blocking(source.read(buffer, 0, buffer.length))
      writtenBytes <- if(readBytes > -1) {
        Sync[F].blocking(destination.write(buffer, 0, readBytes)) >> transmit(source, destination, buffer, transferredBytes + readBytes)
      } else {
        Sync[F].pure(transferredBytes)
      }
    } yield writtenBytes

  private def transfer[F[_] : Sync](inputStream: FileInputStream, outputStream: FileOutputStream): F[Long] =
    transmit(inputStream, outputStream, new Array[Byte](1024 * 10), 0L)

  private def copy[F[_] : Sync](source: File, destination: File): F[Long] =
    inputOutputStream(source, destination).use {
      case (in, out) => transfer(in, out)
    }

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- if( args.length != 2) {
        IO.raiseError(new IllegalArgumentException("Need source and destination files"))
      } else {
        IO.unit
      }
      source = new File(args(0))
      destination = new File(args(1))
      transferredBytes <- copy[IO](source, destination)
      _ <- IO.println(s"$transferredBytes bytes copied from ${source.getPath} to ${destination.getPath}")
    } yield ExitCode.Success
}
