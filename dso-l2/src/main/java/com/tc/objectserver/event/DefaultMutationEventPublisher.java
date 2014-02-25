package com.tc.objectserver.event;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.objectserver.managedobject.CDSMValue;
import com.tc.server.BasicServerEvent;
import com.tc.server.CustomLifespanVersionedServerEvent;
import com.tc.server.ServerEventType;
import com.tc.server.VersionedServerEvent;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Eugene Shelestovich
 */
public class DefaultMutationEventPublisher implements MutationEventPublisher {

  private final Multimap<ObjectID, ServerEventWrapper> pendingEvents = ArrayListMultimap.create();
  // TODO: it would be nice to be able to clear this map once the events are processed. With the current
  // design, we can't do that because we don't know how many events there will be.
  private final Map<ObjectID, byte[]>                    oidToValueMap = Maps.newHashMap();

  private final ServerEventBuffer                      serverEventBuffer;
  private final GlobalTransactionID                      gtxId;

  public DefaultMutationEventPublisher(final GlobalTransactionID gtxId, final ServerEventBuffer serverEventBuffer) {
    this.gtxId = gtxId;
    this.serverEventBuffer = serverEventBuffer;
  }

  @Override
  public void publishEvent(Set<ClientID> clientIds, ServerEventType type, Object key, CDSMValue value, String cacheName) {
    if(clientIds.isEmpty()) {
      return;
    }

    VersionedServerEvent serverEvent = new CustomLifespanVersionedServerEvent(new BasicServerEvent(type, key, value.getVersion(), cacheName),
                                                               (int) value.getCreationTime(), (int) value.getTimeToIdle(),
                                                               (int) value.getTimeToLive());

    if (!value.getObjectID().isNull()) {
      byte[] valueBytes = oidToValueMap.get(value.getObjectID());
      if (valueBytes == null) {
        // We don't have the bytes yet so just make this pending for now.
        pendingEvents.put(value.getObjectID(), new ServerEventWrapper(serverEvent, clientIds));
        return;
      } else {
        serverEvent.setValue(valueBytes);
      }
    }
    serverEventBuffer.storeEvent(gtxId, serverEvent, clientIds);
  }

  @Override
  public void setBytesForObjectID(final ObjectID objectId, final byte[] value) {
    checkState(oidToValueMap.put(objectId, value) == null);
    Collection<ServerEventWrapper> serverEventWrappers = pendingEvents.removeAll(objectId);
    for (ServerEventWrapper wrapper : serverEventWrappers) {
      wrapper.serverEvent.setValue(value);
      serverEventBuffer.storeEvent(gtxId, wrapper.serverEvent, wrapper.clientIds);
    }
  }

  private class ServerEventWrapper {
    private final VersionedServerEvent serverEvent;
    private final Set<ClientID>        clientIds;

    public ServerEventWrapper(VersionedServerEvent serverEvent, Set<ClientID> clientIds) {
      this.serverEvent = serverEvent;
      this.clientIds = clientIds;
    }

  }

}
