/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.management.ManagementEventListener;
import com.tc.management.ManagementResponseListener;
import com.tc.management.TCManagementEvent;
import com.tc.management.TerracottaManagement;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.management.ManagementRequestID;
import com.tc.object.msg.InvokeRegisteredServiceResponseMessage;
import com.tc.object.msg.ListRegisteredServicesResponseMessage;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class ServerManagementHandler extends AbstractEventHandler {

  private static final int MAX_UNFIRED_EVENT_COUNT = 15;
  private static final int MAX_UNFIRED_EVENT_RETENTION_MILLIS = 30000;

  private final Map<ManagementRequestID, ManagementResponseListener> responseListenerMap = new ConcurrentHashMap<ManagementRequestID, ManagementResponseListener>();
  private final List<ManagementEventListener> eventListeners = new CopyOnWriteArrayList<ManagementEventListener>();
  private final List<EventHolder> unfiredEvents = new CopyOnWriteArrayList<EventHolder>() {
    @Override
    public boolean add(EventHolder eventHolder) {
      if (size() >= maxUnfiredEventCount()) {
        remove(0);
      }
      return super.add(eventHolder);
    }
  };

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof ListRegisteredServicesResponseMessage) {
      ListRegisteredServicesResponseMessage response = (ListRegisteredServicesResponseMessage)context;
      ManagementRequestID managementRequestID = response.getManagementRequestID();
      ManagementResponseListener responseListener = responseListenerMap.get(managementRequestID);
      if (responseListener != null) {
        try {
          responseListener.onResponse(response);
        } catch (RuntimeException re) {
          getLogger().warn("response listener threw RuntimeException", re);
        }
      } else {
        getLogger().warn("no listener registered for response " + managementRequestID + " - dropping it");
      }
    } else if (context instanceof InvokeRegisteredServiceResponseMessage) {
      InvokeRegisteredServiceResponseMessage response = (InvokeRegisteredServiceResponseMessage)context;
      NodeID sourceNodeID = response.getSourceNodeID();

      ManagementRequestID managementRequestID = response.getManagementRequestID();

      if (managementRequestID == null) {
        // L1 event
        for (ManagementEventListener eventListener : eventListeners) {
          try {
            Map<String, Object> contextMap = new HashMap<String, Object>();
            ClientID clientID = (ClientID)sourceNodeID;
            contextMap.put(ManagementEventListener.CONTEXT_SOURCE_NODE_NAME, Long.toString(clientID.toLong()));
            contextMap.put(ManagementEventListener.CONTEXT_SOURCE_JMX_ID, TerracottaManagement.buildNodeId(response.getChannel().getRemoteAddress()));
            TCManagementEvent event = (TCManagementEvent)response.getResponseHolder().getResponse(eventListener.getClassLoader());
            eventListener.onEvent(event, contextMap);
          } catch (RuntimeException re) {
            getLogger().warn("event listener threw RuntimeException", re);
          } catch (ClassNotFoundException cnfe) {
            getLogger().warn("received event of an unknown class", cnfe);
          }
        }
      } else {
        // L1 response
        ManagementResponseListener responseListener = responseListenerMap.get(managementRequestID);
        if (responseListener != null) {
          try {
            responseListener.onResponse(response);
          } catch (RuntimeException re) {
            getLogger().warn("response listener threw RuntimeException", re);
          }
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
    boolean empty = eventListeners.isEmpty();
    eventListeners.add(eventListener);
    if (empty) {
      for (EventHolder eventHolder : unfiredEvents) {
        if (!eventHolder.isExpired()) {
          eventListener.onEvent(eventHolder.event, eventHolder.context);
        }
      }
      unfiredEvents.clear();
    }
  }

  public void unregisterEventListener(ManagementEventListener eventListener) {
    eventListeners.remove(eventListener);
  }

  public void fireEvent(TCManagementEvent event, Map<String, Object> context) {
    if (eventListeners.isEmpty()) {
      unfiredEvents.add(new EventHolder(event, context));
    } else {
      for (ManagementEventListener listener : eventListeners) {
        try {
          listener.onEvent(event, context);
        } catch (RuntimeException re) {
          getLogger().warn("Management event listener error", re);
        }
      }
    }
  }


  private final class EventHolder {
    TCManagementEvent event;
    Map<String, Object> context;
    long fireTime;

    private EventHolder(TCManagementEvent event, Map<String, Object> context) {
      this.event = event;
      this.context = context;
      this.fireTime = System.nanoTime();
    }

    boolean isExpired() {
      long now = System.nanoTime();
      return TimeUnit.NANOSECONDS.toMillis(now - fireTime) >= maxUnfiredEventRetentionMillis();
    }
  }

  int maxUnfiredEventRetentionMillis() {
    return MAX_UNFIRED_EVENT_RETENTION_MILLIS;
  }

  int maxUnfiredEventCount() {
    return MAX_UNFIRED_EVENT_COUNT;
  }

}
