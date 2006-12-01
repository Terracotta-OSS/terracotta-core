/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;
import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.bytes.TCByteBuffer;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.SetOnceRef;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * TCConnection implementation that is <b>not </b> specific to a particular IO library and/or JDK release
 *
 * @author teck
 */
abstract class AbstractTCConnection implements TCConnection {

  AbstractTCConnection(TCConnectionEventListener listener, TCProtocolAdaptor adaptor, AbstractTCConnectionManager parent) {
    Assert.assertNotNull(parent);
    Assert.assertNotNull(adaptor);

    this.parent = parent;
    this.protocolAdaptor = adaptor;

    if (listener != null) addListener(listener);

    staticEvent = new TCConnectionEvent() {
      public TCConnection getSource() {
        return AbstractTCConnection.this;
      }

      public String toString() {
        return AbstractTCConnection.this.toString();
      }
    };

    eventFlags[CONNECT] = new SetOnceFlag();
    eventFlags[EOF] = new SetOnceFlag();
    eventFlags[ERROR] = new SetOnceFlag();
    eventFlags[CLOSE] = new SetOnceFlag();

    Assert.assertNoNullElements(eventFlags);
  }

  public final void asynchClose() {
    if (closed.attemptSet()) {
      closeImpl(createCloseCallback(null));
    }
  }

  public final boolean close(final long timeout) {
    if (timeout <= 0) { throw new IllegalArgumentException("timeout cannot be less than or equal to zero"); }

    if (closed.attemptSet()) {
      final Latch latch = new Latch();
      closeImpl(createCloseCallback(latch));
      try {
        return latch.attempt(timeout);
      } catch (InterruptedException e) {
        logger.warn("close interrupted");
        return isConnected();
      }
    }

    return isClosed();
  }

  private Runnable createCloseCallback(final Latch latch) {
    final boolean fireClose = isConnected();

    return new Runnable() {
      public void run() {
        setConnected(false);
        parent.connectionClosed(AbstractTCConnection.this);

        if (fireClose) {
          fireCloseEvent();
        }

        if (latch != null) latch.release();
      }
    };
  }

  public boolean isClosed() {
    return closed.isSet();
  }

  public boolean isConnected() {
    return connected.get();
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();

    buf.append(getClass().getName()).append('@').append(hashCode()).append(":");

    buf.append(" connected: ").append(isConnected());
    buf.append(", closed: ").append(isClosed());

    if (isSocketEndpoint.get()) {
      buf.append(" local=");
      if (localSocketAddress.isSet()) {
        buf.append(((TCSocketAddress) localSocketAddress.get()).getStringForm());
      } else {
        buf.append("[unknown]");
      }

      buf.append(" remote=");
      if (remoteSocketAddress.isSet()) {
        buf.append(((TCSocketAddress) remoteSocketAddress.get()).getStringForm());
      } else {
        buf.append("[unknown");
      }
    }

    buf.append(" connect=[");
    final long connect = getConnectTime();

    if (connect != NO_CONNECT_TIME) {
      buf.append(new Date(connect));
    } else {
      buf.append("no connect time");
    }
    buf.append(']');

    buf.append(" idle=").append(getIdleTime()).append("ms");

    return buf.toString();
  }

  public final void addListener(TCConnectionEventListener listener) {
    if (listener == null) { return; }
    eventListeners.add(listener); // don't need sync
  }

  public final void removeListener(TCConnectionEventListener listener) {
    if (listener == null) { return; }
    eventListeners.remove(listener); // don't need sync
  }

  public final long getConnectTime() {
    return connectTime.get();
  }

  public final long getIdleTime() {
    return System.currentTimeMillis() - lastActivityTime.get();
  }

  public final synchronized void connect(TCSocketAddress addr, int timeout) throws IOException, TCTimeoutException {
    if (closed.isSet() || connected.get()) { throw new IllegalStateException("Connection closed or already connected"); }
    connectImpl(addr, timeout);
    finishConnect();
  }

  public final synchronized boolean asynchConnect(TCSocketAddress addr) throws IOException {
    if (closed.isSet() || connected.get()) { throw new IllegalStateException("Connection closed or already connected"); }

    boolean rv = asynchConnectImpl(addr);

    if (rv) {
      finishConnect();
    }

    return rv;
  }

  public final void putMessage(TCNetworkMessage message) {
    lastActivityTime.set(System.currentTimeMillis());

    // if (!isConnected() || isClosed()) {
    // logger.warn("Ignoring message sent to non-connected connection");
    // return;
    // }

    putMessageImpl(message);
  }

  public final TCSocketAddress getLocalAddress() {
    return (TCSocketAddress) localSocketAddress.get();
  }

  public final TCSocketAddress getRemoteAddress() {
    return (TCSocketAddress) remoteSocketAddress.get();
  }

  abstract protected void closeImpl(Runnable callback);

  abstract protected void putMessageImpl(TCNetworkMessage message);

  abstract protected void connectImpl(TCSocketAddress addr, int timeout) throws IOException, TCTimeoutException;

  abstract protected boolean asynchConnectImpl(TCSocketAddress addr) throws IOException;

  protected void setConnected(boolean connected) {
    if (connected) {
      this.connectTime.set(System.currentTimeMillis());
    }
    this.connected.set(connected);
  }

  protected void recordSocketAddress(Socket socket) {
    if (socket != null) {
      isSocketEndpoint.set(true);
      localSocketAddress.set(new TCSocketAddress(socket.getLocalAddress(), socket.getLocalPort()));
      remoteSocketAddress.set(new TCSocketAddress(socket.getInetAddress(), socket.getPort()));
    }
  }

  protected void addNetworkData(TCByteBuffer[] data, int length) {
    lastActivityTime.set(System.currentTimeMillis());

    try {
      protocolAdaptor.addReadData(this, data, length);
    } catch (Exception e) {
      fireErrorEvent(e, null);
      return;
    }
  }

  protected TCByteBuffer[] getReadBuffers() {
    // TODO: Hook in some form of read throttle. To throttle how much data is read from the network,
    // only return a subset of the buffers that the protocolAdaptor advises to be used.

    // TODO: should also support a way to de-register read interest temporarily

    return protocolAdaptor.getReadBuffers();
  }

  protected void fireErrorEvent(String message) {
    fireErrorEvent(new Exception(message), null);
  }

  protected void fireErrorEvent(String message, TCNetworkMessage context) {
    fireErrorEvent(new Exception(message), context);
  }

  protected void fireErrorEvent(final Exception exception, final TCNetworkMessage context) {
    final TCConnectionErrorEvent event = new TCConnectionErrorEvent() {
      public Exception getException() {
        return exception;
      }

      public TCConnection getSource() {
        return AbstractTCConnection.this;
      }

      public TCNetworkMessage getMessageContext() {
        return context;
      }

      public String toString() {
        return AbstractTCConnection.this + ", exception: "
               + ((exception != null) ? exception.toString() : "[null exception]") + ", message context: "
               + ((context != null) ? context.toString() : "[no message context]");
      }
    };

    fireEvent(ERROR, event);
  }

  protected void fireConnectEvent() {
    fireEvent(CONNECT, staticEvent);
  }

  protected void fireEndOfFileEvent() {
    fireEvent(EOF, staticEvent);
  }

  protected void fireCloseEvent() {
    fireEvent(CLOSE, staticEvent);
  }

  protected void finishConnect() {
    setConnected(true);
    fireConnectEvent();
  }

  public final Socket detach() throws IOException {
    this.parent.removeConnection(this);
    return detachImpl();
  }

  protected abstract Socket detachImpl() throws IOException;

  private void fireEvent(final int type, final TCConnectionEvent event) {
    final SetOnceFlag flag = eventFlags[type];
    Assert.assertNotNull("event flag for type " + type, flag);

    if (flag.attemptSet()) {
      for (Iterator iter = eventListeners.iterator(); iter.hasNext();) {
        TCConnectionEventListener listener = (TCConnectionEventListener) iter.next();
        Assert.assertNotNull("listener", listener);
        try {
          switch (type) {
            case EOF: {
              listener.endOfFileEvent(event);
              break;
            }
            case CLOSE: {
              listener.closeEvent(event);
              break;
            }
            case ERROR: {
              listener.errorEvent((TCConnectionErrorEvent) event);
              break;
            }
            case CONNECT: {
              listener.connectEvent(event);
              break;
            }
            default: {
              throw new InternalError("unknown event type " + type);
            }
          }
        } catch (Exception e) {
          logger.error("Unhandled exception in event handler", e);
        }
      }
    }
  }

  public static final long                  NO_CONNECT_TIME     = -1L;
  protected static final TCLogger           logger              = TCLogging.getLogger(TCConnection.class);

  private static final int                  CONNECT             = 0;
  private static final int                  EOF                 = 1;
  private static final int                  ERROR               = 2;
  private static final int                  CLOSE               = 3;

  private final AbstractTCConnectionManager parent;
  private final SetOnceFlag[]               eventFlags          = new SetOnceFlag[4];
  private final SynchronizedLong            lastActivityTime    = new SynchronizedLong(System.currentTimeMillis());
  private final SynchronizedLong            connectTime         = new SynchronizedLong(NO_CONNECT_TIME);
  private final TCConnectionEvent           staticEvent;
  private final List                        eventListeners      = new CopyOnWriteArrayList();
  private final TCProtocolAdaptor           protocolAdaptor;
  private final SynchronizedBoolean         isSocketEndpoint    = new SynchronizedBoolean(false);
  private final SetOnceFlag                 closed              = new SetOnceFlag();
  private final SynchronizedBoolean         connected           = new SynchronizedBoolean(false);
  private final SetOnceRef                  localSocketAddress  = new SetOnceRef();
  private final SetOnceRef                  remoteSocketAddress = new SetOnceRef();

}