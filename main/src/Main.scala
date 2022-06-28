package vzxplnhqr.workcalcs

import cats.effect._
import scala.concurrent.duration._

object Main extends IOApp.Simple {

    val f = IO.println("dddd") >> IO.println("fffff")
    val run = Blockchain.useBitcoind[IO,Unit]{ 
        implicit blockchain => {
            blockchain.blocks.foreach{ block =>
                IO.println(s"processing block ${block.blockHeader.hashBE.hex}") >> IO.sleep(1.second)
            }.compile.drain
        }      
    }

}