/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.memorydatastore.client;

import com.tc.exception.TCRuntimeException;
import com.tc.memorydatastore.message.MemoryDataStoreRequestMessage;
import com.tc.memorydatastore.message.MemoryDataStoreResponseMessage;
import com.tc.memorydatastore.server.MemoryDataStoreServer;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.locks.ThreadID;
import com.tc.object.session.NullSessionManager;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MemoryDataStoreClient implements MemoryDataMap {
  private final static CommunicationsManager communicationsManager = new CommunicationsManagerImpl(
                                                                                                   "MemoryDataStoreClient",
                                                                                                   new NullMessageMonitor(),
                                                                                                   new PlainNetworkStackHarnessFactory(),
                                                                                                   new NullConnectionPolicy(),
                                                                                                   0);

  private ClientMessageChannel               channel;
  private final Map                          pendingRequests       = new HashMap();
  private final Map                          waitObjectMap         = new HashMap();
  private final Map                          pendingResponses      = new HashMap();
  private final String                       storeName;
  private final ThreadLocal                  threadID              = new ThreadLocal();

  private long                               threadIDSequence;

  public MemoryDataStoreClient(final String storeName, final String serverHost, final int serverPort) {
    super();
    this.storeName = storeName;
    setupClient(serverHost, serverPort);
  }

  public MemoryDataStoreClient(final String storeName) {
    this(storeName, "localhost", MemoryDataStoreServer.DEFAULT_PORT);
  }

  private void setupClient(final String serverHost, final int serverPort) {

    this.channel = communicationsManager
        .createClientChannel(new NullSessionManager(), -1, serverHost, serverPort, 10000,
                             new ConnectionAddressProvider(new ConnectionInfo[] { new ConnectionInfo(serverHost,
                                                                                                     serverPort) }));

    this.channel
        .addClassMapping(TCMessageType.MEMORY_DATA_STORE_RESPONSE_MESSAGE, MemoryDataStoreResponseMessage.class);
    this.channel.addClassMapping(TCMessageType.MEMORY_DATA_STORE_REQUEST_MESSAGE, MemoryDataStoreRequestMessage.class);
    this.channel.routeMessageType(TCMessageType.MEMORY_DATA_STORE_RESPONSE_MESSAGE,
                                  new MemoryDataStoreResponseSink(this));

    while (true) {
      try {
        this.channel.open();
        break;
      } catch (final TCTimeoutException tcte) {
        ThreadUtil.reallySleep(5000);
      } catch (final ConnectException e) {
        ThreadUtil.reallySleep(5000);
      } catch (final MaxConnectionsExceededException e) {
        ThreadUtil.reallySleep(5000);
      } catch (final CommStackMismatchException e) {
        ThreadUtil.reallySleep(5000);
      } catch (final IOException ioe) {
        ThreadUtil.reallySleep(5000);
      }
    }
  }

  public void close() {
    this.channel.close();
  }

  private synchronized long nextThreadID() {
    return ++this.threadIDSequence;
  }

  private ThreadID getThreadID() {
    ThreadID rv = (ThreadID) this.threadID.get();
    if (rv == null) {
      rv = new ThreadID(nextThreadID());
      this.threadID.set(rv);
    }

    return rv;
  }

  public void put(final byte[] key, final byte[] value) {
    final ThreadID thId = getThreadID();
    final MemoryDataStoreRequestMessage request = (MemoryDataStoreRequestMessage) this.channel
        .createMessage(TCMessageType.MEMORY_DATA_STORE_REQUEST_MESSAGE);
    request.initializePut(thId, this.storeName, key, value);
    request.send();
    // MemoryDataStoreResponseMessage responseMessage =
    // waitForResponse(threadID, request);
    // Assert.assertTrue(responseMessage.isRequestCompletedFlag());
  }

  public byte[] get(final byte[] key) {
    final ThreadID thId = getThreadID();
    final MemoryDataStoreRequestMessage request = (MemoryDataStoreRequestMessage) this.channel
        .createMessage(TCMessageType.MEMORY_DATA_STORE_REQUEST_MESSAGE);
    request.initializeGet(thId, this.storeName, key, false);

    final Object waitObject = getWaitObject(thId, request);

    request.send();
    final MemoryDataStoreResponseMessage responseMessage = waitForResponse(thId, waitObject);
    Assert.assertTrue(responseMessage.isRequestCompletedFlag());
    return responseMessage.getValue();
  }

  public Collection getAll(final byte[] key) {
    final ThreadID thId = getThreadID();
    final MemoryDataStoreRequestMessage request = (MemoryDataStoreRequestMessage) this.channel
        .createMessage(TCMessageType.MEMORY_DATA_STORE_REQUEST_MESSAGE);
    request.initializeGet(thId, this.storeName, key, true);

    final Object waitObject = getWaitObject(thId, request);

    request.send();
    final MemoryDataStoreResponseMessage responseMessage = waitForResponse(thId, waitObject);
    Assert.assertTrue(responseMessage.isRequestCompletedFlag());
    return responseMessage.getValues();
  }

  public Collection getAll() {
    final byte[] emptyKey = new byte[0];
    return (getAll(emptyKey));
  }

  public void remove(final byte[] key) {
    final ThreadID thId = getThreadID();
    final MemoryDataStoreRequestMessage request = (MemoryDataStoreRequestMessage) this.channel
        .createMessage(TCMessageType.MEMORY_DATA_STORE_REQUEST_MESSAGE);
    request.initializeRemove(thId, this.storeName, key, false);
    request.send();
    // MemoryDataStoreResponseMessage responseMessage =
    // waitForResponse(threadID, request);
    // Assert.assertTrue(responseMessage.isRequestCompletedFlag());
    // return responseMessage.getValue();
  }

  public void removeAll(final byte[] key) {
    final ThreadID thId = getThreadID();
    final MemoryDataStoreRequestMessage request = (MemoryDataStoreRequestMessage) this.channel
        .createMessage(TCMessageType.MEMORY_DATA_STORE_REQUEST_MESSAGE);
    request.initializeRemove(thId, this.storeName, key, true);
    request.send();
    // MemoryDataStoreResponseMessage responseMessage =
    // waitForResponse(threadID, request);
    // Assert.assertTrue(responseMessage.isRequestCompletedFlag());
    // return responseMessage.getNumOfRemove();
  }

  void notifyResponse(final ThreadID thId, final MemoryDataStoreResponseMessage response) {
    Object waitObject = null;
    synchronized (this) {
      waitObject = this.waitObjectMap.get(thId);
      final Object pendingRequest = this.pendingRequests.remove(thId);
      this.pendingResponses.put(thId, response);
      Assert.assertNotNull(waitObject);
      Assert.assertNotNull(pendingRequest);
    }
    synchronized (waitObject) {
      waitObject.notifyAll();
    }
  }

  private MemoryDataStoreResponseMessage waitForResponse(final ThreadID thId, final Object waitObject) {
    synchronized (waitObject) {
      while (hasPendingRequest(thId)) {
        try {
          waitObject.wait();
        } catch (final InterruptedException e) {
          throw new TCRuntimeException(e);
        }
      }
    }
    synchronized (this) {
      final MemoryDataStoreResponseMessage responseMessage = (MemoryDataStoreResponseMessage) this.pendingResponses
          .remove(thId);
      Assert.assertNotNull(responseMessage);
      return responseMessage;
    }
  }

  private boolean hasPendingRequest(final ThreadID thId) {
    return this.pendingRequests.get(thId) != null;
  }

  private synchronized Object getWaitObject(final ThreadID thId, final TCMessage message) {
    final Object waitObject = new Object();
    this.waitObjectMap.put(thId, waitObject);
    this.pendingRequests.put(thId, message);
    return waitObject;
  }
}
