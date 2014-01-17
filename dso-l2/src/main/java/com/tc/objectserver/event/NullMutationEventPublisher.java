package com.tc.objectserver.event;

import com.tc.object.ObjectID;
import com.tc.objectserver.managedobject.CDSMValue;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;

import java.util.Collections;
import java.util.List;

/**
 * @author Eugene Shelestovich
 */
public class NullMutationEventPublisher implements MutationEventPublisher {

  @Override
  public void setBytesForObjectID(final ObjectID objectId, final byte[] value) {
  }

  @Override
  public void publishEvent(ServerEventType type, Object key, CDSMValue value, String cacheName) {
  }
}
