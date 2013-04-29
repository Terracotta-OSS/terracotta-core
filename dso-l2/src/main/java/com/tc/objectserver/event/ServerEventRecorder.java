/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.event;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.tc.object.ObjectID;
import com.tc.object.ServerEventType;
import com.tc.objectserver.impl.SamplingType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Records all cache modifications and maps keys to actual values
 * omiting intermediate object id.
 *
 * @author Eugene Shelestovich
 */
public final class ServerEventRecorder {

  private final List<IntermediateForm> events = new ArrayList<IntermediateForm>();
  private final Map<ObjectID, byte[]> oidToValueMap = new HashMap<ObjectID, byte[]>();

  public void recordEvent(final ServerEventType type, final Object key, final ObjectID objectId,
                          final String cacheName) {
    events.add(new IntermediateForm(new BasicServerEvent(type, key, cacheName), objectId));
  }

  public void recordEventValue(final ObjectID objectId, final byte[] value) {
    oidToValueMap.put(objectId, value);
  }

  /**
   * Transforms removals into evictions or expirations.
   */
  public void reconsiderRemovals(final SamplingType samplingType) {
    for (final IntermediateForm event : events) {
      if (event.target.getType() == ServerEventType.REMOVE) {
        switch (samplingType) {
          case FOR_EVICTION:
            event.target.setType(ServerEventType.EVICT);
            break;
          case FOR_EXPIRATION:
            event.target.setType(ServerEventType.EXPIRE);
            break;
        }
      }
    }
  }

  public List<ServerEvent> getEvents() {
    return Lists.transform(events, new FormToEventFunction());
  }

  private final class FormToEventFunction implements Function<IntermediateForm, ServerEvent> {
    @Override
    public ServerEvent apply(final IntermediateForm form) {
      final byte[] value = oidToValueMap.get(form.objectId);
      if (value != null) {
        form.target.setValue(value);
      }
      return form.target;
    }
  }

  private static final class IntermediateForm {
    private final ServerEvent target;
    private final ObjectID objectId;

    private IntermediateForm(final ServerEvent target, final ObjectID objectId) {
      this.target = target;
      this.objectId = objectId;
    }
  }
}
