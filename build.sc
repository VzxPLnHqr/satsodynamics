import mill._, scalalib._

object main extends ScalaModule {

  def scalaVersion = "2.13.8"
  
  def ivyDeps = Agg(
    ivy"org.typelevel::cats-effect:3.3.12",
    ivy"org.bitcoin-s::bitcoin-s-bitcoind-rpc:1.9.1",
    ivy"co.fs2::fs2-io:3.2.8", // for streaming
    ivy"org.typelevel::spire:0.18.0", // for number types
    ivy"org.slf4j:slf4j-nop:1.7.12" // to get rid of a weird logging warning
  )

  def scalacPluginIvyDeps = Agg(
    ivy"com.olegpy::better-monadic-for::0.3.1"
  )

  def mainClass = Some("vzxplnhqr.workcalcs.Main")
}