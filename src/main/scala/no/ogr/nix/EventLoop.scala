package no.ogr.nix

import java.nio.channels.spi.SelectorProvider
import java.nio.channels.{SelectableChannel, SelectionKey}
import scala.concurrent.duration._
import rx.lang.scala.{Observer, Subscription, Observable}

trait SelectService {
  def select[T <: SelectableChannel](channel: T, interestOps: Int): Observable[Event[T]]
}

class EventLoop(selectorProvider: SelectorProvider = SelectorProvider.provider()) extends SelectService {
  private val selector = selectorProvider.openSelector()
  private val timeout = 1.second
  private var running = true

  def stop() {
    running = false
  }

  private def handleOps(set: java.util.Set[SelectionKey]) {
    val i = set.iterator()
    while (i.hasNext) {
      val key = i.next()
      val observer = Option(key.attachment().asInstanceOf[Observer[Event[_]]])

      if (key.isAcceptable) {
        observer.foreach(_.onNext(AcceptEvent(this, key.channel())))
      }

      if (key.isConnectable) {
        observer.foreach(_.onNext(ConnectEvent(this, key.channel())))
      }

      if (key.isReadable) {
        observer.foreach(_.onNext(ReadEvent(this, key.channel())))
      }

      if (key.isWritable) {
        observer.foreach(_.onNext(WriteEvent(this, key.channel())))
      }

      i.remove()
    }
  }

  def select[T <: SelectableChannel](channel: T, interestOps: Int): Observable[Event[T]] = {
    val key = channel.register(selector, interestOps)
    Observable.create {
      observer =>
        val oldObserver = Option(key.attach(observer).asInstanceOf[Observer[_]])
        oldObserver.foreach(_.onCompleted())
        new Subscription {
          override def unsubscribe() {
            key.cancel()
          }
        }
    }
  }

  def run() {
    while (running) {
      val opsCount = selector.select(timeout.toMillis)
      if (opsCount > 0) {
        handleOps(selector.selectedKeys())
      }
    }
  }
}
