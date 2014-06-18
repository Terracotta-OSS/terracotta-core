/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.management.ManagementRequestID;
import com.tc.object.management.RemoteCallDescriptor;
import com.tc.object.management.RemoteCallHolder;
import com.tc.object.management.ResponseHolder;
import com.tc.object.management.TCManagementSerializationException;
import com.tc.object.msg.AbstractManagementMessage;
import com.tc.object.msg.InvokeRegisteredServiceMessage;
import com.tc.object.msg.InvokeRegisteredServiceResponseMessage;
import com.tc.object.msg.ListRegisteredServicesMessage;
import com.tc.object.msg.ListRegisteredServicesResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.handler.ServerManagementHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Ludovic Orban
 */
public class RemoteManagementImpl implements RemoteManagement {

  private final DSOChannelManager channelManager;
  private final ServerManagementHandler serverManagementHandler;
  private final String thisServerNodeName;


  public RemoteManagementImpl(DSOChannelManager channelManager, ServerManagementHandler serverManagementHandler, String thisServerNodeName) {
    this.channelManager = channelManager;
    this.serverManagementHandler = serverManagementHandler;
    this.thisServerNodeName = thisServerNodeName;
  }

  @Override
  public void sendEvent(TCManagementEvent event) {
    Map<String, Object> context = new HashMap<String, Object>();
    context.put(ManagementEventListener.CONTEXT_SOURCE_NODE_NAME, thisServerNodeName);
    serverManagementHandler.fireEvent(event, context);
  }

  @Override
  public void registerEventListener(ManagementEventListener listener) {
    serverManagementHandler.registerEventListener(listener);
  }

  @Override
  public void unregisterEventListener(ManagementEventListener listener) {
    serverManagementHandler.unregisterEventListener(listener);
  }

  public Set<NodeID> getAllClientIDs() {
    return channelManager.getAllClientIDs();
  }

  public Set<RemoteCallDescriptor> listRegisteredServices(NodeID node, long timeout, TimeUnit unit) throws RemoteManagementException {
    ListRegisteredServicesMessage message;
    try {
      message = (ListRegisteredServicesMessage)channelManager.getActiveChannel(node)
          .createMessage(TCMessageType.LIST_REGISTERED_SERVICES_MESSAGE);
    } catch (NoSuchChannelException nsce) {
      throw new RemoteManagementException("Error listing registered management services", nsce);
    }

    final AtomicReference<Set<RemoteCallDescriptor>> remoteCallDescriptors = new AtomicReference<Set<RemoteCallDescriptor>>();

    final CountDownLatch latch = new CountDownLatch(1);
    serverManagementHandler.registerResponseListener(message.getManagementRequestID(), new ManagementResponseListener() {
      @Override
      public void onResponse(AbstractManagementMessage mgmtMessage) {
        ListRegisteredServicesResponseMessage response = (ListRegisteredServicesResponseMessage)mgmtMessage;

        remoteCallDescriptors.set(response.getRemoteCallDescriptors());

        serverManagementHandler.unregisterResponseListener(mgmtMessage.getManagementRequestID());
        latch.countDown();
      }
    });
    message.send();

    try {
      if (!latch.await(timeout, unit)) {
        serverManagementHandler.unregisterResponseListener(message.getManagementRequestID());
        throw new RemoteManagementException("Timed out while waiting for listRegisteredServices() response");
      }
    } catch (InterruptedException ie) {
      serverManagementHandler.unregisterResponseListener(message.getManagementRequestID());
      throw new RemoteManagementException("Interrupted while waiting for listRegisteredServices() response", ie);
    }

    return remoteCallDescriptors.get();
  }


  public Future<Object> asyncRemoteCall(final RemoteCallDescriptor remoteCallDescriptor, final ClassLoader classLoader, Object... args) throws RemoteManagementException {
    InvokeRegisteredServiceMessage invokeMessage;
    try {
      invokeMessage = (InvokeRegisteredServiceMessage)channelManager.getActiveChannel(remoteCallDescriptor.getL1Node())
          .createMessage(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE);
    } catch (NoSuchChannelException nsce) {
      throw new RemoteManagementException("Error calling management services", nsce);
    }

    final AtomicReference<Exception> exception = new AtomicReference<Exception>();
    final AtomicReference<Object> response = new AtomicReference<Object>();

    RemoteCallHolder remoteCallHolder = new RemoteCallHolder(remoteCallDescriptor, args);
    invokeMessage.setRemoteCallHolder(remoteCallHolder);

    final AtomicReference<ManagementRequestID> responseManagementRequestIDRef = new AtomicReference<ManagementRequestID>();
    final CountDownLatch latch = new CountDownLatch(1);
    serverManagementHandler.registerResponseListener(invokeMessage.getManagementRequestID(), new ManagementResponseListener() {
      @Override
      public void onResponse(AbstractManagementMessage message) {
        InvokeRegisteredServiceResponseMessage responseMessage = (InvokeRegisteredServiceResponseMessage)message;
        ResponseHolder responseHolder = responseMessage.getResponseHolder();
        try {
          exception.set(responseHolder.getException(classLoader));
          response.set(responseHolder.getResponse(classLoader));
        } catch (ClassNotFoundException cnfe) {
          exception.set(new TCManagementSerializationException("Error deserializing management response", cnfe));
          response.set(null);
        }
        ManagementRequestID managementRequestID = message.getManagementRequestID();
        responseManagementRequestIDRef.set(managementRequestID);
        serverManagementHandler.unregisterResponseListener(managementRequestID);
        latch.countDown();
      }
    });
    final ManagementRequestID sentManagementRequestID = invokeMessage.getManagementRequestID();
    invokeMessage.send();

    return new Future<Object>() {
      private volatile boolean cancelled = false;

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        if (cancelled) {
          return true;
        } else if (!isDone()) {
          cancelled = true;
          serverManagementHandler.unregisterResponseListener(sentManagementRequestID);
          latch.countDown(); // make it done
          return true;
        } else {
          return false;
        }
      }

      @Override
      public boolean isCancelled() {
        return cancelled;
      }

      @Override
      public boolean isDone() {
        return latch.getCount() == 0;
      }

      @Override
      public Object get() throws InterruptedException, ExecutionException {
        latch.await();
        if (isCancelled()) {
          throw new CancellationException("Management remote L1 call on " + remoteCallDescriptor.getL1Node() + " got cancelled");
        }
        if (exception.get() != null) {
          throw new ExecutionException(new RemoteManagementException("Error performing management remote L1 call on " + remoteCallDescriptor.getL1Node(), exception.get()));
        }
        return response.get();
      }

      @Override
      public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!latch.await(timeout, unit)) {
          throw new TimeoutException("Timed out waiting for management remote L1 call response");
        }
        if (isCancelled()) {
          throw new CancellationException("Management remote L1 call on " + remoteCallDescriptor.getL1Node() + " got cancelled");
        }
        if (exception.get() != null) {
          throw new ExecutionException(new RemoteManagementException("Error performing management remote L1 call on " + remoteCallDescriptor.getL1Node(), exception.get()));
        }
        return response.get();
      }
    };
  }

}
