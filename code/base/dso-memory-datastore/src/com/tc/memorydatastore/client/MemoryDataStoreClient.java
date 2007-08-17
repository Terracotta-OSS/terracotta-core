/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.memorydatastore.client;

import com.tc.exception.TCRuntimeException;
import com.tc.memorydatastore.message.MemoryDataStoreRequestMessage;
import com.tc.memorydatastore.message.MemoryDataStoreResponseMessage;
import com.tc.memorydatastore.server.MemoryDataStoreServer;
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
import com.tc.object.lockmanager.api.ThreadID;
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
                                                                       new NullMessageMonitor(),
                                                                       new PlainNetworkStackHarnessFactory(),
                                                                       new NullConnectionPolicy());

  private ClientMessageChannel               channel;
  private final Map                          pendingRequests       = new HashMap();
  private final Map                          waitObjectMap         = new HashMap();
  private final Map                          pendingResponses      = new HashMap();
  private final String                       storeName;
  private final ThreadLocal                  threadID              = new ThreadLocal();

  private long                               threadIDSequence;

  public MemoryDataStoreClient(String storeName, String serverHost, int serverPort) {
    super();
    this.storeName = storeName;
    setupClient(serverHost, serverPort);
  }

  public MemoryDataStoreClient(String storeName) {
    this(storeName, "localhost", MemoryDataStoreServer.DEFAULT_PORT);
  }

  private void setupClient(String serverHost, int serverPort) {

    this.channel = communicationsManager.createClientChannel(new NullSessionManager(), -1, serverHost, serverPort,
        10000, new ConnectionAddressProvider(new ConnectionInfo[] { new ConnectionInfo(serverHost, serverPort) }));

    channel.addClassMapping(TCMessageType.MEMORY_DATA_STORE_RESPONSE_MESSAGE, MemoryDataStoreResponseMessage.class);
    channel.addClassMapping(TCMessageType.MEMORY_DATA_STORE_REQUEST_MESSAGE, MemoryDataStoreRequestMessage.class);
    channel.routeMessageType(TCMessageType.MEMORY_DATA_STORE_RESPONSE_MESSAGE, new MemoryDataStoreResponseSink(this));

    while (true) {
      try {
        channel.open();
        break;
      } catch (TCTimeoutException tcte) {
        ThreadUtil.reallySleep(5000);
      } catch (ConnectException e) {
        ThreadUtil.reallySleep(5000);
      } catch (MaxConnectionsExceededException e) {
        ThreadUtil.reallySleep(5000);
      } catch (IOException ioe) {
        ioe.printStackTrace();
        throw new RuntimeException(ioe);
      }
    }
  }

  public void close() {
    channel.close();
  }

  private synchronized long nextThreadID() {
    return ++threadIDSequence;
  }

  private ThreadID getThreadID() {
    ThreadID rv = (ThreadID) threadID.get();
    if (rv == null) {
      rv = new ThreadID(nextThreadID());
      threadID.set(rv);
    }

    return rv;
  }

  public void put(byte[] key, byte[] value) {
    ThreadID thId = getThreadID();
    MemoryDataStoreRequestMessage request = (MemoryDataStoreRequestMessage) channel
        .createMessage(TCMessageType.MEMORY_DATA_STORE_REQUEST_MESSAGE);
    request.initializePut(thId, this.storeName, key, value);
    request.send();
    // MemoryDataStoreResponseMessage responseMessage =
    // waitForResponse(threadID, request);
    // Assert.assertTrue(responseMessage.isRequestCompletedFlag());
  }

  public byte[] get(byte[] key) {
    ThreadID thId = getThreadID();
    MemoryDataStoreRequestMessage request = (MemoryDataStoreRequestMessage) channel
        .createMessage(TCMessageType.MEMORY_DATA_STORE_REQUEST_MESSAGE);
    request.initializeGet(thId, this.storeName, key, false);

    Object waitObject = getWaitObject(thId, request);

    request.send();
    MemoryDataStoreResponseMessage responseMessage = waitForResponse(thId, waitObject);
    Assert.assertTrue(responseMessage.isRequestCompletedFlag());
    return responseMessage.getValue();
  }

  public Collection getAll(byte[] key) {
    ThreadID thId = getThreadID();
    MemoryDataStoreRequestMessage request = (MemoryDataStoreRequestMessage) channel
        .createMessage(TCMessageType.MEMORY_DATA_STORE_REQUEST_MESSAGE);
    request.initializeGet(thId, this.storeName, key, true);

    Object waitObject = getWaitObject(thId, request);

    request.send();
    MemoryDataStoreResponseMessage responseMessage = waitForResponse(thId, waitObject);
    Assert.assertTrue(responseMessage.isRequestCompletedFlag());
    return responseMessage.getValues();
  }
  
  public Collection getAll() {
    byte[] emptyKey = new byte[0];
    return(getAll(emptyKey));
  }

  public void remove(byte[] key) {
    ThreadID thId = getThreadID();
    MemoryDataStoreRequestMessage request = (MemoryDataStoreRequestMessage) channel
        .createMessage(TCMessageType.MEMORY_DATA_STORE_REQUEST_MESSAGE);
    request.initializeRemove(thId, this.storeName, key, false);
    request.send();
    // MemoryDataStoreResponseMessage responseMessage =
    // waitForResponse(threadID, request);
    // Assert.assertTrue(responseMessage.isRequestCompletedFlag());
    // return responseMessage.getValue();
  }

  public void removeAll(byte[] key) {
    ThreadID thId = getThreadID();
    MemoryDataStoreRequestMessage request = (MemoryDataStoreRequestMessage) channel
        .createMessage(TCMessageType.MEMORY_DATA_STORE_REQUEST_MESSAGE);
    request.initializeRemove(thId, this.storeName, key, true);
    request.send();
    // MemoryDataStoreResponseMessage responseMessage =
    // waitForResponse(threadID, request);
    // Assert.assertTrue(responseMessage.isRequestCompletedFlag());
    // return responseMessage.getNumOfRemove();
  }

  void notifyResponse(ThreadID thId, MemoryDataStoreResponseMessage response) {
    Object waitObject = null;
    synchronized (this) {
      waitObject = this.waitObjectMap.get(thId);
      Object pendingRequest = this.pendingRequests.remove(thId);
      this.pendingResponses.put(thId, response);
      Assert.assertNotNull(waitObject);
      Assert.assertNotNull(pendingRequest);
    }
    synchronized (waitObject) {
      waitObject.notifyAll();
    }
  }

  private MemoryDataStoreResponseMessage waitForResponse(ThreadID thId, Object waitObject) {
    synchronized (waitObject) {
      while (hasPendingRequest(thId)) {
        try {
          waitObject.wait();
        } catch (InterruptedException e) {
          throw new TCRuntimeException(e);
        }
      }
    }
    synchronized (this) {
      MemoryDataStoreResponseMessage responseMessage = (MemoryDataStoreResponseMessage) this.pendingResponses
          .remove(thId);
      Assert.assertNotNull(responseMessage);
      return responseMessage;
    }
  }

  private boolean hasPendingRequest(ThreadID thId) {
    return this.pendingRequests.get(thId) != null;
  }

  private synchronized Object getWaitObject(ThreadID thId, TCMessage message) {
    Object waitObject = new Object();
    this.waitObjectMap.put(thId, waitObject);
    this.pendingRequests.put(thId, message);
    return waitObject;
  }
}
