package example.filecopier

import cats.effect.{ExitCode, IO, IOApp, Resource}

import java.io._

object FileCopier extends IOApp {

  private def inputStream(file: File): Resource[IO, FileInputStream] =
    Resource.make {
      IO.blocking(new FileInputStream(file))
    } { inputStream =>
      IO.blocking(inputStream.close()).handleErrorWith(_ => IO.unit)
    }

  private def outputStream(file: File): Resource[IO, FileOutputStream] =
    Resource.make {
      IO.blocking(new FileOutputStream(file))
    } {
      outputStream =>
        IO.blocking(outputStream.close()).handleErrorWith(_ => IO.unit)
    }

  private def inputOutputStream(inputFile: File, outputFile: File): Resource[IO, (FileInputStream, FileOutputStream)] =
    for {
      in <- inputStream(inputFile)
      out <- outputStream(outputFile)
    } yield (in, out)

  private def transmit(source: InputStream, destination: OutputStream, buffer: Array[Byte], transferredBytes: Long): IO[Long] =
    for {
      readBytes <- IO.blocking(source.read(buffer, 0, buffer.length))
      writtenBytes <- if(readBytes > -1) {
        IO.blocking(destination.write(buffer, 0, readBytes)) >> transmit(source, destination, buffer, transferredBytes + readBytes)
      } else {
        IO.pure(transferredBytes)
      }
    } yield writtenBytes

  private def transfer(inputStream: FileInputStream, outputStream: FileOutputStream): IO[Long] =
    transmit(inputStream, outputStream, new Array[Byte](1024 * 10), 0L)

  private def copy(source: File, destination: File): IO[Long] =
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
      transferredBytes <- copy(source, destination)
      _ <- IO.println(s"$transferredBytes bytes copied from ${source.getPath} to ${destination.getPath}")
    } yield ExitCode.Success
}
