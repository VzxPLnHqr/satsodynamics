package vzxplnhqr.workcalcs

import cats.effect._

// for storing/retreiving secrets and config information in application.conf
import com.typesafe.config._

// simple configuration algebra
trait MyConfig[F[_]]{
    def defaultConfig: F[Config]
    def bitcoind_rpcuser: F[String]
    def bitcoind_rpcpassword: F[String]
}

object MyConfig {
    def apply[F[_] : MyConfig]: MyConfig[F] = implicitly

    implicit val myConfigIO = new MyConfig[IO] {
        // surely a better way to do this, but oh well.
        // application.conf must contain the following settings:
        //    bitcoin-rpc.rpcuser = "user"
        //    bitcoin-rpc.rpcpassword = "password"
        def defaultConfig = IO(ConfigFactory
                                .parseString(
                                    scala.io.Source.fromFile("./application.conf")
                                        .getLines().mkString("\n"))
        )
        def bitcoind_rpcuser = defaultConfig.map(_.getString("bitcoind-rpc.rpcuser"))
        def bitcoind_rpcpassword = defaultConfig.map(_.getString("bitcoind-rpc.rpcpassword"))
    }
}