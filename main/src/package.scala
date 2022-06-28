package vzxplnhqr

import cats.effect._

package object workcalcs {
    //helper method for converting Future[A] to IO[A] without so many prenthesis
    implicit class futureToIO[A](future: => scala.concurrent.Future[A]){
        def toF[F[_] : Async]: F[A] = Async[F].fromFuture(Async[F].delay(future))
    }
}