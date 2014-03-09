package no.ogr.nix

import java.net.InetSocketAddress
import java.nio.channels.{SocketChannel, SelectionKey, ServerSocketChannel}
import rx.lang.scala.{Observer, Observable}
import no.ogr.nix.SocketServer.PipelineFactory

class SocketServer(eventLoop: EventLoop,
                   channel: ServerSocketChannel,
                   clientPipeline: PipelineFactory)
{
  channel.configureBlocking(false)

  private val acceptObserver = new Observer[Event[ServerSocketChannel]] {
    override def onNext(event: Event[ServerSocketChannel]) {
      val channel    = event.channel.accept()
      channel.configureBlocking(false)
      val observable = event.service.select(channel, SelectionKey.OP_READ)
      clientPipeline(observable)
    }
  }
  val subscription = eventLoop.select(channel, SelectionKey.OP_ACCEPT).subscribe(acceptObserver)
}

object SocketServer {
  type PipelineFactory = (Observable[Event[SocketChannel]]) => Unit

  def apply(eventLoop: EventLoop, address: InetSocketAddress, backlog: Int)(factory: PipelineFactory): SocketServer = {
    val channel = ServerSocketChannel.open().bind(address, backlog)
    new SocketServer(eventLoop, channel, factory)
  }
}

