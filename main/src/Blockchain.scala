package vzxplnhqr.workcalcs

import cats._
import cats.implicits._
import cats.effect._
import std.Console

import org.bitcoins._
import org.bitcoins.core.protocol.blockchain.Block
import org.bitcoins.core.protocol.transaction.Transaction

import fs2.Stream

// algebra to capture queries of the chain
// hides the complexities of actually performing the queries
// we use the data structures from the bitcoin-s library
// but since most of the time bitcoin-s rpc calls returns Futures, we
// instead build our own interpreter which unwraps those futures and puts them
// in a more purely functional IO context
trait Blockchain[F[_]] {
    def getBlockCount: F[Int]
    def getBlock(height: Int): F[Block]
}

object Blockchain {
    def apply[F[_] : Blockchain]: Blockchain[F] = implicitly

    def blocks[F[_] : Blockchain](implicit monadCancel: MonadCancel[F,_]): Stream[F,Block] = for {
        n <- Stream.eval(Blockchain[F].getBlockCount)
        i <- Stream.range(start = 1, stopExclusive = n+1)
        b <- Stream.eval(Blockchain[F].getBlock(i))
    } yield b

    def transactions[F[_]:Blockchain](implicit monadCancel: MonadCancel[F,_]): Stream[F,Transaction] = for {
        b <- blocks[F]
        t <- Stream.chunk(fs2.Chunk.seq(b.transactions))
    } yield t

    def countBlocks[F[_] : Blockchain]: Stream[F,Int] = Stream.eval(Blockchain[F].getBlockCount)

    object examplePrograms {
        def prog2[F[_]:Blockchain:Console:Monad] = for {
            n <- Blockchain[F].getBlockCount
            b <- Blockchain[F].getBlock(n)
            difficulty = b.blockHeader.difficulty
            _ <- Console[F].println(s"block $n has target $difficulty")
            target = Blockchain.nBits2target(b.blockHeader.nBits)
            _ <- Console[F].println(s"our calculation     $target")
        } yield difficulty
    }

    // this implicit class just provides easier syntactic access to the polymorphic
    // methods supplied above
    implicit class BlockchainProgs[F[_]](blockchainF: Blockchain[F]) {
        implicit val implBlockchainF = blockchainF
        def blocks(implicit monadCancel: MonadCancel[F,_]): Stream[F,Block] = Blockchain.blocks

        def countBlocks: Stream[F,Int] = Stream.eval[F,Int](blockchainF.getBlockCount)
    }

    /**
      * Takes the nBits difficulty from the blockheader and converts it to the
      * "target" value, which is a 256-bit number. A valid block must achieve
      * a hash which, when interpreted as a 256-bit number, is below this
      * target.
      *
      * @param nBits
      * @return 256-bit number
      */
    def nBits2target(nBits: org.bitcoins.core.number.UInt32): BigInt = {
        // from https://blog.devfans.io/math-formulas-in-bitcoin-mining/
        //difficulty = difficulty_1_target / current_target
        //for bitcoin difficulty_1_target = 0x1d00ffff
        //sign = nbits & 0x00800000
        //mantissa = (nbits & 0x7fffff)
        //exponent = nbits >> 24
        //target = (-1^sign) * mantissa * 256^(exponent-3)
        val sign = nBits & org.bitcoins.core.number.UInt32("0x00800000")
        val mantissa = nBits & org.bitcoins.core.number.UInt32("0x7fffff")
        val exponent = nBits >> 24
        (BigInt(-1).pow(sign.toInt))*(mantissa.toBigInt) * BigInt(256).pow((exponent - org.bitcoins.core.number.UInt32(BigInt(3))).toInt)
    }

    /**
      * An simple implemenation for the IO context
      * using a Bitcoind monad implementation
      */
    def blockchainM[F[_] : Bitcoind : Async]:Blockchain[F] =
        new Blockchain[F] {
            def getBlock(height: Int): F[Block] = Bitcoind[F].useRpcCli{
                cli => for {
                    h <- cli.getBlockHash(height).toF[F]
                    b <- cli.getBlockRaw(h).toF[F]
                } yield b
            }

            def getBlockCount: F[Int] = Bitcoind[F].useRpcCli{_.getBlockCount.toF[F]}
        }

    def useBitcoind[F[_] : Bitcoind : Async,B](f: Blockchain[F] => F[B]) =
        Resource.make(Async[F].delay(blockchainM[F]))(_ => Bitcoind[F].cleanup()).use(f)
}