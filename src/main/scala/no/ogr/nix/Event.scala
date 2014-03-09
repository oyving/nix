package no.ogr.nix

import java.nio.channels.SelectableChannel

sealed trait Event[T <: SelectableChannel] {
  val channel: T
  val service: SelectService
}
case class ReadEvent[T <: SelectableChannel](service: SelectService, channel: T) extends Event[T]
case class WriteEvent[T <: SelectableChannel](service: SelectService, channel: T) extends Event[T]
case class ConnectEvent[T <: SelectableChannel](service: SelectService, channel: T) extends Event[T]
case class AcceptEvent[T <: SelectableChannel](service: SelectService, channel: T) extends Event[T]
