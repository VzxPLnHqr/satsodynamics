package vzxplnhqr.workcalcs

import cats.effect._
import org.bitcoins.rpc.client.common.BitcoindRpcClient
import org.bitcoins.rpc.config.{BitcoindAuthCredentials,BitcoindInstanceRemote}
import scala.concurrent.Future

trait Bitcoind[F[_]]{
    // we want to make it extremely easy to query the bitcoind rpc interface
    // but the bitcoin-s rpc client uses an actor system under the hood which
    // has some housekeeping we do not want to have to care about. Here we
    // instead abstract the rpc client up into a general F[_] context and leave
    // the housekeeping to the implementation.

    /**
      * Use the bitcoind rpc client similar to how cats-effect Resource#use
      * works. An example for IO is below. Note: the implementation will spin
      * up and tear down a whole actor system, so `useRpcCli` is intended to
      * be used only once in your program:
        ```
            object Main extends IOApp.Simple {
                val run = Bitcoind[IO].useRpcCli{ 
                    cli => for {
                    n <-  cli.getBlockCount.toF[IO]
                    h <-  cli.getBlockHeader(n).toF[IO]
                    } yield ()
                }
            }
        ```
      */
    def useRpcCli[B](f: BitcoindRpcClient => F[B]):F[B]
    def cleanup(): F[Unit] //clean up any resources (actor system, etc)
}

object Bitcoind {
    def apply[F[_] : Bitcoind]: Bitcoind[F] = implicitly

    /**
      * Now some example programs using Bitcoind rpc client. Notice
      * how the underlying plumbing has been abstracted away.
      */
    object examplePrograms {
        import cats._
        import cats.implicits._
        import cats.effect.std.Console

        def testGetBlockCount[F[_] : Bitcoind : Console : Async : Monad] = 
            Bitcoind[F].useRpcCli{
                cli => for {
                    _ <- Console[F].println("starting up...")
                    n <- cli.getBlockCount.toF[F]
                    _ <- Console[F].println(s"block count is $n")
                } yield ()
            }

        /**
          *     val run2 = for {
                _ <- IO.println("hello bitcoin!")
                n <- Blockchain[IO].getBlockCount
                _ <- IO.println(s"the current block count is: $n")
                block_n <- Blockchain[IO].getBlockWithTransactions(n)
                _ <- IO.println(s"block $n has ${block_n.tx.length} transactions")
                _ <- IO.println(s"the chainwork is ${block_n.chainwork}")
                nSats <- IO(block_n.tx(0).vout.map(_.value.satoshis.toLong).sum)
                _ <- IO.println(s"number of sats in the coinbase output is $nSats")
                d = block_n.difficulty
                nBits = block_n.bits
                t = Blockchain.nBits2target(nBits)
                _ <- IO.println(s"difficulty (from block): $d")
                _ <- IO.println(s"target (from nbits): ${t.toString(16)}")
                difficulty_1_target = Blockchain.nBits2target(org.bitcoins.core.number.UInt32("0x1d00ffff"))
                difficulty = BigDecimal(difficulty_1_target) / BigDecimal(t)
                _ <- IO.println(s"difficulty (our calc): $difficulty")
                _ <- IO.println("cleaning up") >> Blockchain[IO].cleanup()
                _ <- IO.println("bye!")
            } yield ()
          */   
    }


    /**
      * A basic implementation using the cats-effect IO context
      */
    implicit val bitcoindIO = new Bitcoind[IO] {
        private val authCredentials = for {
            u <- MyConfig[IO].bitcoind_rpcuser
            p <- MyConfig[IO].bitcoind_rpcpassword
            r = BitcoindAuthCredentials.PasswordBased(
                username = u,
                password = p
            )
        } yield r

        // bitcoin-s uses an akka actor system under the hood, but we do not
        // care, so thankfully/hopefully we can hide its existence here
        val actorSystem = akka.actor.ActorSystem("System")

        private val _rpcCli = for {
                creds <- authCredentials
                actorSys = actorSystem
                bitcoindInstance = //IO(
                                // remember to forward the port
                                // ssh -L 8332:localhost:8332 user@server 
                                BitcoindInstanceRemote(
                                    network = org.bitcoins.core.config.MainNet,
                                    uri = new java.net.URI(s"http://localhost:${org.bitcoins.core.config.MainNet.port}"),
                                    rpcUri = new java.net.URI(s"http://localhost:${org.bitcoins.core.config.MainNet.rpcPort}"),
                                    authCredentials = creds
                                )(actorSys)
                            //)
                r = BitcoindRpcClient.withActorSystem(bitcoindInstance)(actorSys)
            } yield r

        def cleanup():IO[Unit] = 
            IO.println("cleaning up...") >> _rpcCli.map(_.system.terminate()).as(()) >> IO.println("bye!")

        /*
        private def _rpcCli = Resource.make(
            for {
                creds <- authCredentials
                actorSys = actorSystem
                bitcoindInstance = //IO(
                                // remember to forward the port
                                // ssh -L 8332:localhost:8332 user@server 
                                BitcoindInstanceRemote(
                                    network = org.bitcoins.core.config.MainNet,
                                    uri = new java.net.URI(s"http://localhost:${org.bitcoins.core.config.MainNet.port}"),
                                    rpcUri = new java.net.URI(s"http://localhost:${org.bitcoins.core.config.MainNet.rpcPort}"),
                                    authCredentials = creds
                                )(actorSys)
                            //)
                r = BitcoindRpcClient.withActorSystem(bitcoindInstance)(actorSys)
            } yield r
        ) {
            //cleanup the actor system after use
            c => IO.println("cleaning up...") >> IO(c.system.terminate()).as(())
        }*/

        /**
          * Use the bitcoind rpc client. Should only be called once.
          * `BitcoindRpcClient` from bitcoin-s often returns a Future,
          * and is evaluated eagerly. This is frustrating. To get better
          * semantics, wrap the future `x` in IO.fromFuture(IO(`x`))
          *
          * @param f
          * @return
          */
        def useRpcCli[B](f: BitcoindRpcClient => IO[B]): IO[B] = _rpcCli.flatMap(f) //_rpcCli.use(f)
    }
}