/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.management.ManagementEventListener;
import com.tc.management.ManagementResponseListener;
import com.tc.object.management.ManagementRequestID;
import com.tc.object.msg.AbstractManagementMessage;
import com.tc.object.msg.InvokeRegisteredServiceResponseMessage;
import com.tc.object.msg.ListRegisteredServicesResponseMessage;
import com.tc.util.Assert;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerManagementHandler extends AbstractEventHandler {

  private final Map<ManagementRequestID, ManagementResponseListener> responseListenerMap = new ConcurrentHashMap<ManagementRequestID, ManagementResponseListener>();
  private final List<ManagementEventListener> eventListeners = new CopyOnWriteArrayList<ManagementEventListener>();

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof ListRegisteredServicesResponseMessage) {
      ListRegisteredServicesResponseMessage response = (ListRegisteredServicesResponseMessage)context;
      ManagementRequestID managementRequestID = response.getManagementRequestID();
      ManagementResponseListener responseListener = responseListenerMap.get(managementRequestID);
      if (responseListener != null) {
        responseListener.onResponse(response);
      } else {
        getLogger().warn("no listener registered for response " + managementRequestID + " - dropping it");
      }
    } else if (context instanceof InvokeRegisteredServiceResponseMessage) {
      InvokeRegisteredServiceResponseMessage response = (InvokeRegisteredServiceResponseMessage)context;

      ManagementRequestID managementRequestID = response.getManagementRequestID();

      if (managementRequestID == null) {
        // L1 event
        for (ManagementEventListener eventListener : eventListeners) {
          try {
            Serializable event = (Serializable)response.getResponseHolder().getResponse(eventListener.getClassLoader());
            eventListener.onEvent(event);
          } catch (ClassNotFoundException cnfe) {
            getLogger().warn("received event of an unknown class", cnfe);
          }
        }
      } else {
        // L1 response
        ManagementResponseListener responseListener = responseListenerMap.get(managementRequestID);
        if (responseListener != null) {
          responseListener.onResponse(response);
        } else {
          getLogger().warn("no listener registered for response " + managementRequestID + " - dropping it");
        }
      }
    } else {
      Assert.fail("Unknown event type " + context.getClass().getName());
    }
  }

  public void registerResponseListener(ManagementRequestID managementRequestID, ManagementResponseListener responseListener) {
    responseListenerMap.put(managementRequestID, responseListener);
  }

  public void unregisterResponseListener(ManagementRequestID managementRequestID) {
    responseListenerMap.remove(managementRequestID);
  }

  public void registerEventListener(ManagementEventListener eventListener) {
    eventListeners.add(eventListener);
  }

  public void unregisterEventListener(ManagementEventListener eventListener) {
    eventListeners.remove(eventListener);
  }

}
