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
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapGetValueRequest;
import com.tc.object.ServerMapRequestType;
import com.tc.object.msg.GetAllKeysServerMapRequestMessage;
import com.tc.object.msg.GetAllSizeServerMapRequestMessage;
import com.tc.object.msg.GetValueServerMapRequestMessage;
import com.tc.object.net.ChannelStats;
import com.tc.objectserver.api.ServerMapRequestManager;
import com.tc.objectserver.context.ServerMapGetAllSizeHelper;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.stats.counter.Counter;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

public class ServerMapRequestHandler extends AbstractEventHandler {

  private ServerMapRequestManager serverMapRequestManager;
  private final Counter           globalGetValueRequestCounter;
  private final Counter           globalGetSizeRequestCounter;
  private final Counter           globalGetSnapshotRequestCounter;
  private ChannelStats            channelStats;

  public ServerMapRequestHandler(Counter globalGetSizeRequestCounter, Counter globalGetValueRequestCounter,
                                 Counter globalGetSnapshotRequestCounter) {
    this.globalGetSizeRequestCounter = globalGetSizeRequestCounter;
    this.globalGetValueRequestCounter = globalGetValueRequestCounter;
    this.globalGetSnapshotRequestCounter = globalGetSnapshotRequestCounter;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof GetAllSizeServerMapRequestMessage) {
      final GetAllSizeServerMapRequestMessage smContext = (GetAllSizeServerMapRequestMessage) context;
      globalGetSizeRequestCounter.increment();
      this.channelStats.notifyServerMapRequest(ServerMapRequestType.GET_SIZE, smContext.getChannel(), 1);
      ServerMapGetAllSizeHelper helper = new ServerMapGetAllSizeHelper(smContext.getMaps());
      for (ObjectID mid : smContext.getMaps()) {
        this.serverMapRequestManager.requestSize(smContext.getRequestID(), smContext.getClientID(), mid, helper);
      }
    } else if (context instanceof GetValueServerMapRequestMessage) {
      final GetValueServerMapRequestMessage smContext = (GetValueServerMapRequestMessage) context;
      final Map<ObjectID, Collection<ServerMapGetValueRequest>> requests = smContext.getRequests();
      int numRequests = requests.size();
      globalGetValueRequestCounter.increment(numRequests);
      this.channelStats.notifyServerMapRequest(ServerMapRequestType.GET_VALUE_FOR_KEY, smContext.getChannel(),
                                               numRequests);
      for (final Entry<ObjectID, Collection<ServerMapGetValueRequest>> e : requests.entrySet()) {
        this.serverMapRequestManager.requestValues(smContext.getClientID(), e.getKey(), e.getValue());
      }
    } else if (context instanceof GetAllKeysServerMapRequestMessage) {
      final GetAllKeysServerMapRequestMessage smContext = (GetAllKeysServerMapRequestMessage) context;
      globalGetSnapshotRequestCounter.increment();
      this.channelStats.notifyServerMapRequest(ServerMapRequestType.GET_ALL_KEYS, smContext.getChannel(), 1);
      this.serverMapRequestManager.requestAllKeys(smContext.getRequestID(), smContext.getClientID(), smContext
          .getMapID());
    } else {
      throw new AssertionError("Unknown message type: " + context.getClass());
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    final ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.serverMapRequestManager = oscc.getServerMapRequestManager();
    this.channelStats = oscc.getChannelStats();
  }

}
