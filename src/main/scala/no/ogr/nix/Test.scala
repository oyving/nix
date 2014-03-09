package no.ogr.nix

import java.net.InetSocketAddress
import rx.lang.scala.Observer
import java.nio.channels.{SocketChannel, SelectionKey, ServerSocketChannel}
import java.nio.ByteBuffer

object HelloTest {
  def main(args: Array[String]) {
    val eventLoop = new EventLoop()
    SocketServer(eventLoop, new InetSocketAddress(8080)) {
      observable =>
        observable.subscribe(new Observer[Event[SocketChannel]] {
          override def onNext(event: Event[SocketChannel]) {
            event.channel.write(ByteBuffer.wrap("Hello, World!\n".getBytes("UTF-8")))
            event.channel.close()
          }
        })
    }
    eventLoop.run()
  }
}

object EchoTest {
  val echoObserver = new Observer[Event[SocketChannel]] {
    override def onNext(event: Event[SocketChannel]) {
      val buffer = ByteBuffer.allocate(1024)

      var size = 0
      do {
        size = event.channel.read(buffer)
      } while (size > 0)

      buffer.flip()
      event.channel.write(buffer)

      buffer.clear()
    }
  }

  def main(args: Array[String]) {
    val eventLoop = new EventLoop()
    SocketServer(eventLoop, new InetSocketAddress(8080)) {
      observable =>
        observable.subscribe(echoObserver)
    }
    eventLoop.run()
  }
}