/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCInternalError;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogging;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.TCExceptionResultException;
import com.tc.util.concurrent.TCFuture;

import java.io.IOException;
import java.util.List;

/**
 * Client implementation of the transport network layer.
 */
public class ClientMessageTransport extends MessageTransportBase {
  // it was 2 minutes timeout, reduce to 10 sec
  private static final long                 SYN_ACK_TIMEOUT = 10000;
  private final ClientConnectionEstablisher connectionEstablisher;
  private boolean                           wasOpened       = false;
  private TCFuture                          waitForSynAckResult;
  private final WireProtocolAdaptorFactory  wireProtocolAdaptorFactory;
  private final SynchronizedBoolean         isOpening       = new SynchronizedBoolean(false);

  /**
   * Constructor for when you want a transport that isn't connected yet (e.g., in a client). This constructor will
   * create an unopened MessageTransport.
   * 
   * @param commsManager CommmunicationsManager
   */
  public ClientMessageTransport(ClientConnectionEstablisher clientConnectionEstablisher,
                                TransportHandshakeErrorHandler handshakeErrorHandler,
                                TransportHandshakeMessageFactory messageFactory,
                                WireProtocolAdaptorFactory wireProtocolAdaptorFactory) {

    super(MessageTransportState.STATE_START, handshakeErrorHandler, messageFactory, false,  TCLogging.getLogger(ClientMessageTransport.class));
    this.wireProtocolAdaptorFactory = wireProtocolAdaptorFactory;
    this.connectionEstablisher = clientConnectionEstablisher;
  }

  /**
   * Blocking open. Causes a connection to be made. Will throw exceptions if the connect fails.
   * 
   * @throws TCTimeoutException
   * @throws IOException
   * @throws TCTimeoutException
   * @throws MaxConnectionsExceededException
   */
  public NetworkStackID open() throws TCTimeoutException, IOException, MaxConnectionsExceededException {
    // XXX: This extra boolean flag is dumb, but it's here because the close event can show up
    // while the lock on isOpen is held here. That will cause a deadlock because the close event is thrown on the
    // comms thread which means that the handshake messages can't be sent.
    // The state machine here needs to be rationalized.
    isOpening.set(true);
    synchronized (isOpen) {
      Assert.eval("can't open an already open transport", !isOpen.get());
      try {
        wireNewConnection(connectionEstablisher.open(this));
        HandshakeResult result = handShake();
        if (result.isMaxConnectionsExceeded()) {
          // Hack to make the connection clear
          // but don't do all the gunk around reconnect
          // clean this up
          List tl = this.getTransportListeners();
          this.removeTransportListeners();
          clearConnection();
          this.addTransportListeners(tl);
          status.reset();
          throw new MaxConnectionsExceededException("Maximum number of client connections exceeded: "
                                                    + result.maxConnections());
        }
        Assert.eval(!this.connectionId.isNull());
        isOpen.set(true);
        sendAck();
        NetworkStackID nid =  new NetworkStackID(this.connectionId.getChannelID());
        wasOpened = true;
        return(nid);
      } catch (TCTimeoutException e) {
        status.reset();
        throw e;
      } catch (IOException e) {
        status.reset();
        throw e;
      } finally {
        isOpening.set(false);
      }
    }
  }

  /**
   * Returns true if the MessageTransport was ever in an open state.
   */
  public boolean wasOpened() {
    return wasOpened;
  }

  public boolean isNotOpen() {
    return !isOpening.get() && !isOpen.get();
  }

  // TODO :: come back
  public void closeEvent(TCConnectionEvent event) {
    if (isNotOpen()) return;
    status.reset();
    super.closeEvent(event);
  }

  protected void receiveTransportMessageImpl(WireProtocolMessage message) {
    synchronized (status) {
      if (status.isSynSent()) {
        handleSynAck(message);
        message.recycle();
        return;
      }
    }
    super.receiveToReceiveLayer(message);
  }

  private void handleSynAck(WireProtocolMessage message) {
    if (!verifySynAck(message)) {
      handleHandshakeError(new TransportHandshakeErrorContext(
                                                              "Received a message that was not a SYN_ACK while waiting for SYN_ACK: "
                                                                  + message));
    } else {
      SynAckMessage synAck = (SynAckMessage) message;
      if (synAck.hasErrorContext()) { throw new ImplementMe(synAck.getErrorContext()); }

      if (connectionId != null && !ConnectionID.NULL_ID.equals(connectionId)) {
        // This is a reconnect
        Assert.eval(connectionId.equals(synAck.getConnectionId()));
      }
      if (!synAck.isMaxConnectionsExceeded()) {
        this.connectionId = synAck.getConnectionId();

        Assert.assertNotNull("Connection id from the server was null!", this.connectionId);
        Assert.eval(!ConnectionID.NULL_ID.equals(this.connectionId));
        Assert.assertNotNull(this.waitForSynAckResult);
      }
      this.waitForSynAckResult.set(synAck);
    }

    return;
  }

  private boolean verifySynAck(TCNetworkMessage message) {
    // XXX: yuck.
    return message instanceof TransportHandshakeMessage && ((TransportHandshakeMessage) message).isSynAck();
  }

  /**
   * Builds a protocol stack and tries to make a connection. This is a blocking call.
   * 
   * @throws TCTimeoutException
   * @throws MaxConnectionsExceededException
   * @throws IOException
   */
  HandshakeResult handShake() throws TCTimeoutException {
    sendSyn();
    SynAckMessage synAck = waitForSynAck();
    return new HandshakeResult(synAck.isMaxConnectionsExceeded(), synAck.getMaxConnections());
  }

  private SynAckMessage waitForSynAck() throws TCTimeoutException {
    try {
      SynAckMessage synAck = (SynAckMessage) waitForSynAckResult.get(SYN_ACK_TIMEOUT);
      return synAck;
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    } catch (TCExceptionResultException e) {
      throw new TCInternalError(e);
    }
  }

  private void sendSyn() {
    synchronized (status) {
      if (status.isEstablished() || status.isSynSent()) { throw new AssertionError(" ERROR !!! " + status); }
      waitForSynAckResult = new TCFuture(status);
      TransportHandshakeMessage syn = this.messageFactory.createSyn(this.connectionId, getConnection());
      // send syn message
      this.sendToConnection(syn);
      this.status.synSent();
    }
  }

  private void sendAck() {
    synchronized (status) {
      Assert.eval(status.isSynSent());
      TransportHandshakeMessage ack = this.messageFactory.createAck(this.connectionId, getConnection());
      // send ack message
      this.sendToConnection(ack);
      this.status.established();
      fireTransportConnectedEvent();
    }
  }

  void reconnect(TCConnection connection) throws Exception {
    
    // don't do reconnect if open is still going on
    if(!wasOpened) return;
    
    Assert.eval(!isConnected());
    wireNewConnection(connection);
    try {
      HandshakeResult result = handShake();
      sendAck();
      if (result.isMaxConnectionsExceeded()) {
        close();
        throw new MaxConnectionsExceededException(getMaxConnectionsExceededMessage(result.maxConnections()));
      }
    } catch (Exception t) {
      status.reset();
      throw t;
    }
  }

  private String getMaxConnectionsExceededMessage(int maxConnections) {
    return "Maximum number of client connections exceeded: " + maxConnections;
  }

  TCProtocolAdaptor getProtocolAdapter() {
    return wireProtocolAdaptorFactory.newWireProtocolAdaptor(new WireProtocolMessageSink() {
      public void putMessage(WireProtocolMessage message) {
        receiveTransportMessage(message);
      }
    });
  }

  void endIfDisconnected() {
    synchronized (this.status) {
      if (!this.isConnected() && !this.status.isEnd()) {
        this.status.end();
      }
    }
  }

  private static final class HandshakeResult {
    private final boolean maxConnectionsExceeded;
    private final int     maxConnections;

    private HandshakeResult(boolean maxConnectionsExceeded, int maxConnections) {
      this.maxConnectionsExceeded = maxConnectionsExceeded;
      this.maxConnections = maxConnections;
    }

    public int maxConnections() {
      return this.maxConnections;
    }

    public boolean isMaxConnectionsExceeded() {
      return this.maxConnectionsExceeded;
    }
  }

  public ClientConnectionEstablisher getConnectionEstablisher() {
    return connectionEstablisher;
  }
}
