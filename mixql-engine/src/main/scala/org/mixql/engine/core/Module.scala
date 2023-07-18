package org.mixql.engine.core

import com.github.nscala_time.time.Imports.{DateTime, richReadableInstant, richReadableInterval}
import org.mixql.engine.core.logger.ModuleLogger
import org.zeromq.{SocketType, ZMQ}
import org.mixql.protobuf.ProtoBufConverter
import org.mixql.protobuf.messages

import scala.concurrent.{Await, Future}
import scala.language.postfixOps

object Module {
  def sendMsgToServerBroker(msg: Array[Byte])(implicit
                                              server: ZMQ.Socket,
                                              identity: String,
                                              clientAddress: Array[Byte],
                                              logger: ModuleLogger
  ): Boolean = {
    // Sending multipart message
    import logger._
    logDebug(s"sendMsgToServerBroker: sending empty frame")
    server.send("".getBytes(), ZMQ.SNDMORE) // Send empty frame
    logDebug(s"sendMsgToServerBroker: sending clientaddress")
    server.send(clientAddress, ZMQ.SNDMORE) // First send address frame
    logDebug(s"sendMsgToServerBroker: sending empty frame")
    server.send("".getBytes(), ZMQ.SNDMORE) // Send empty frame
    logDebug(s"sendMsgToServerBroker: sending message")
    server.send(msg)
  }

  def sendMsgToServerBroker(
                             msg: String
                           )(implicit
                             server: ZMQ.Socket,
                             identity: String,
                             logger: ModuleLogger
                           ): Boolean = {
    import logger._
    logDebug(
      s"sendMsgToServerBroker: convert msg of type String to Array of bytes"
    )
    logDebug(s"sending empty frame")
    server.send("".getBytes(), ZMQ.SNDMORE) // Send empty frame
    logDebug(s"Send msg to server ")
    server.send(msg.getBytes())
  }

  def sendMsgToServerBroker(
                             clientAdrress: Array[Byte],
                             msg: messages.Message
                           )(implicit
                             server: ZMQ.Socket,
                             identity: String,
                             clientAddress: Array[Byte],
                             logger: ModuleLogger
                           ): Boolean = {
    import logger._
    logDebug(
      s"sendMsgToServerBroker: convert msg of type Protobuf to Array of bytes"
    )
    sendMsgToServerBroker(ProtoBufConverter.toArray(msg))
  }

  def readMsgFromServerBroker()(implicit
                                server: ZMQ.Socket,
                                identity: String,
                                logger: ModuleLogger
  ): (Array[Byte], Option[Array[Byte]], Option[String]) = {
    import logger._
    // FOR PROTOCOL SEE BOOK OReilly ZeroMQ Messaging for any applications 2013 ~page 100
    // From server broker messanger we get msg with such body:
    // indentity frame
    // empty frame --> delimiter
    // data ->
    if (server.recv(0) == null)
      throw new BrakeException() // empty frame
    logDebug(s"readMsgFromServerBroker: received empty frame")

    val clientAdrress = server.recv(0) // Indentity of client object on server
    // or pong-heartbeat from broker
    if (clientAdrress == null)
      throw new BrakeException()

    var msg: Option[Array[Byte]] = None

    var pongHeartMessage: Option[String] = Some(new String(clientAdrress))
    if (pongHeartMessage.get != "PONG-HEARTBEAT") {
      pongHeartMessage = None

      logDebug(
        s"readMsgFromServerBroker: got client address: " + new String(
          clientAdrress
        )
      )

      if (server.recv(0) == null)
        throw new BrakeException() // empty frame
      logDebug(s"readMsgFromServerBroker: received empty frame")

      logDebug(
        s"have received message from server ${new String(clientAdrress)}"
      )
      msg = Some(server.recv(0))
    }

    (clientAdrress, msg, pongHeartMessage)
  }
}

class Module(
              executor: IModuleExecutor,
              indentity: String,
              host: String,
              port: Int
            )(implicit logger: ModuleLogger) {

  import Module._

  var ctx: ZMQ.Context = null
  implicit var server: ZMQ.Socket = null
  var poller: ZMQ.Poller = null

  val heartBeatInterval: Long = 3000
  var processStart: DateTime = null
  var liveness: Int = 3
  var brokerClientAdress: Array[Byte] = Array()

  import logger._

  def startServer(): Unit = {
    logInfo(s"Starting main client")

    logInfo(
      s"host of server is " + host + " and port is " + port.toString
    )

    try {
      ctx = ZMQ.context(1)
      server = ctx.socket(SocketType.DEALER)
      // set identity to our socket, if it would not be set,
      // then it would be generated by ROUTER socket in broker object on server

      server.setIdentity(indentity.getBytes)
      logInfo(
        s"connected: " + server
          .connect(s"tcp://$host:${port.toString}")
      )
      logInfo(s"Connection established.")

      logDebug(s"Module $indentity:Setting processStart for timer")
      // Set timer
      processStart = DateTime.now()

      logInfo(s"Module $indentity:Setting poller")
      poller = ctx.poller(1)
      logInfo(s"Module $indentity:Register pollin in poller")
      val pollInIndex = poller.register(server, ZMQ.Poller.POLLIN)

      logInfo(s"Sending READY message to server's broker")
      implicit val identity = this.indentity
      sendMsgToServerBroker("READY")

      while (true) {
        val rc = poller.poll(heartBeatInterval)
        //        if (rc == 1) throw BrakeException()
        if (poller.pollin(pollInIndex)) {
          logDebug("Setting processStart for timer, as message was received")
          val (clientAdrressTmp, msg, pongHeartBeatMsg) =
            readMsgFromServerBroker()
          pongHeartBeatMsg match {
            case Some(_) => // got pong heart beat message
              logDebug(
                s"got pong heart beat message from broker server"
              )
            case None => // got protobuf message
              implicit val clientAddress = clientAdrressTmp
              brokerClientAdress = clientAddress
              implicit val clientAddressStr = new String(clientAddress)
              //              executor.reactOnMessage(msg.get)(server, identity, clientAddress)
              ProtoBufConverter.unpackAnyMsgFromArray(msg.get) match {
                case msg: messages.Execute =>
                  try {
                    sendMsgToServerBroker(clientAddress,
                      executor.reactOnExecute(msg, identity, clientAddressStr, logger)
                    )(server, identity, clientAddress, logger)
                  } catch {
                    case e: Throwable =>
                      sendMsgToServerBroker(
                        clientAddress,
                        new messages.Error(
                          s"Module $identity to ${clientAddressStr}: error while reacting on execute: " +
                            e.getMessage
                        )
                      )(server, identity, clientAddress, logger)
                  }
                case msg: messages.SetParam =>
                  try {
                    sendMsgToServerBroker(clientAddress,
                      executor.reactOnSetParam(msg, identity, clientAddressStr, logger)
                    )(server, identity, clientAddress, logger)
                  } catch {
                    case e: Throwable =>
                      sendMsgToServerBroker(
                        clientAddress,
                        new messages.Error(
                          s"Module $identity to ${clientAddressStr}: error while reacting on setting param: " +
                            e.getMessage
                        )
                      )(server, identity, clientAddress, logger)
                  }
                case msg: messages.GetParam =>
                  try {
                    sendMsgToServerBroker(clientAddress,
                      executor.reactOnGetParam(msg, identity, clientAddressStr, logger)
                    )(server, identity, clientAddress, logger)
                  } catch {
                    case e: Throwable =>
                      sendMsgToServerBroker(
                        clientAddress,
                        new messages.Error(
                          s"Module $identity to ${clientAddressStr}: error while reacting on getting param: " +
                            e.getMessage
                        )
                      )(server, identity, clientAddress, logger)
                  }
                case msg: messages.IsParam =>
                  try {
                    sendMsgToServerBroker(clientAddress,
                      executor.reactOnIsParam(msg, identity, clientAddressStr, logger)
                    )(server, identity, clientAddress, logger)
                  } catch {
                    case e: Throwable =>
                      sendMsgToServerBroker(
                        clientAddress,
                        new messages.Error(
                          s"Module $identity to ${clientAddressStr}: error while reacting on is param: " +
                            e.getMessage
                        )
                      )(server, identity, clientAddress, logger)
                  }
                case _: messages.ShutDown =>
                  logInfo(s"Started shutdown")
                  try {
                    executor.reactOnShutDown(identity, clientAddressStr, logger)
                  } catch {
                    case e: Throwable => logWarn("Warning: error while reacting on shutdown: " +
                      e.getMessage
                    )
                  }
                  throw new BrakeException()
                case msg: messages.ExecuteFunction =>
                  try {
                    sendMsgToServerBroker(clientAddress,
                      executor.reactOnExecuteFunction(msg, identity, clientAddressStr, logger)
                    )(server, identity, clientAddress, logger)
                  }
                  catch {
                    case e: Throwable =>
                      sendMsgToServerBroker(
                        clientAddress,
                        new messages.Error(
                          s"Module $identity to ${clientAddressStr}: error while reacting on execute function" +
                            s"${msg.name}: " + e.getMessage
                        )
                      )(server, identity, clientAddress, logger)
                  }
                case _: messages.GetDefinedFunctions =>
                  try {
                    sendMsgToServerBroker(clientAddress,
                      executor.reactOnGetDefinedFunctions(identity, clientAddressStr, logger)
                    )(server, identity, clientAddress, logger)
                  }
                  catch {
                    case e: Throwable =>
                      sendMsgToServerBroker(
                        clientAddress,
                        new messages.Error(
                          s"Module $identity to ${clientAddressStr}: error while reacting on getting" +
                            " functions list" + e.getMessage
                        )
                      )(server, identity, clientAddress, logger)
                  }
                case m: messages.Error =>
                  sendMsgToServerBroker(clientAddress, m)(server, identity, clientAddress, logger)
              }
          }
          processStart = DateTime.now()
          liveness = 3
        } else {
          val elapsed = (processStart to DateTime.now()).millis
          logDebug(s"elapsed: " + elapsed)
          liveness = liveness - 1
          if (liveness == 0) {
            logError(
              s"heartbeat failure, can't reach server's broker. Shutting down"
            )
            throw new BrakeException()
          }
          if (elapsed >= heartBeatInterval) {
            processStart = DateTime.now()
            logDebug(
              s"heartbeat work. Sending heart beat. Liveness: " + liveness
            )
            sendMsgToServerBroker("PING-HEARTBEAT")
          }
        }
      }
    } catch {
      case _: BrakeException => logDebug(s"BrakeException")
      case ex: Exception =>
        logError(s"Error: " + ex.getMessage)
        sendMsgToServerBroker(
          brokerClientAdress,
          new messages.Error(
            s"Module $indentity to broker ${brokerClientAdress}: fatal error: " +
              ex.getMessage
          )
        )(server, indentity, brokerClientAdress, logger)
    } finally {
      close()
    }
    logInfo(s"Stopped.")
  }

  def close(): Unit = {
    if (server != null) {
      logInfo(s"finally close server")
      server.close()
    }

    if (poller != null) {
      logInfo(s"finally close poller")
      poller.close()
    }

    try {
      if (ctx != null) {
        logInfo(s"finally close context")
        implicit val ec: scala.concurrent.ExecutionContext =
          scala.concurrent.ExecutionContext.global
        Await.result(
          Future {
            ctx.term()
          },
          scala.concurrent.duration.Duration(5000, "millis")
        )
      }
    } catch {
      case _: Throwable =>
        logError(s"tiemout of closing context exceeded:(")
    }
  }
}
