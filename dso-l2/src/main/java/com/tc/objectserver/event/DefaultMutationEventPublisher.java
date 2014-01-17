package com.tc.objectserver.event;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.tc.object.ObjectID;
import com.tc.objectserver.managedobject.CDSMValue;
import com.tc.server.BasicServerEvent;
import com.tc.server.CustomLifespanVersionedServerEvent;
import com.tc.server.ServerEventType;
import com.tc.server.VersionedServerEvent;

import java.util.Collection;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author Eugene Shelestovich
 */
public class DefaultMutationEventPublisher implements MutationEventPublisher {

  private final Multimap<ObjectID, VersionedServerEvent> pendingEvents = ArrayListMultimap.create();
  // TODO: it would be nice to be able to clear this map once the events are processed. With the current
  // design, we can't do that because we don't know how many events there will be.
  private final Map<ObjectID, byte[]> oidToValueMap = Maps.newHashMap();

  private final ServerEventPublisher serverEventPublisher;

  public DefaultMutationEventPublisher(final ServerEventPublisher serverEventPublisher) {
    this.serverEventPublisher = serverEventPublisher;
  }

  @Override
  public void publishEvent(ServerEventType type, Object key, CDSMValue value, String cacheName) {
    VersionedServerEvent serverEvent = new CustomLifespanVersionedServerEvent(new BasicServerEvent(type, key, value.getVersion(), cacheName),
                                                               (int) value.getCreationTime(), (int) value.getTimeToIdle(),
                                                               (int) value.getTimeToLive());

    if (!value.getObjectID().isNull()) {
      byte[] valueBytes = oidToValueMap.get(value.getObjectID());
      if (valueBytes == null) {
        // We don't have the bytes yet so just make this pending for now.
        pendingEvents.put(value.getObjectID(), serverEvent);
        return;
      } else {
        serverEvent.setValue(valueBytes);
      }
    }
    serverEventPublisher.post(serverEvent);
  }

  @Override
  public void setBytesForObjectID(final ObjectID objectId, final byte[] value) {
    checkState(oidToValueMap.put(objectId, value) == null);
    Collection<VersionedServerEvent> serverEvents = pendingEvents.removeAll(objectId);
    for (VersionedServerEvent serverEvent : serverEvents) {
      serverEvent.setValue(value);
      serverEventPublisher.post(serverEvent);
    }
  }
}
