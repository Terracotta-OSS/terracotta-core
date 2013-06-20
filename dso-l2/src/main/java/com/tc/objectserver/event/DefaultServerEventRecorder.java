package com.tc.objectserver.event;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tc.object.ObjectID;
import com.tc.objectserver.impl.SamplingType;
import com.tc.server.BasicServerEvent;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;
import com.tc.server.VersionedServerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Shelestovich
 */
public class DefaultServerEventRecorder implements ServerEventRecorder {

  private final List<IntermediateForm> events = new ArrayList<IntermediateForm>();
  private final Map<ObjectID, byte[]> oidToValueMap = Maps.newHashMap();

  @Override
  public void recordEvent(final ServerEventType type, final Object key, final ObjectID objectId,
                          final String cacheName) {
    events.add(new IntermediateForm(new BasicServerEvent(type, key, cacheName), objectId));
  }

  @Override
  public void recordEvent(final ServerEventType type, final Object key, final ObjectID objectId,
                          final long version, final String cacheName) {
    events.add(new IntermediateForm(new BasicServerEvent(type, key, version, cacheName), objectId));
  }

  @Override
  public void recordEventValue(final ObjectID objectId, final byte[] value) {
    oidToValueMap.put(objectId, value);
  }

  /**
   * Transforms removals into evictions or expirations.
   */
  @Override
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

  @Override
  public List<ServerEvent> getEvents() {
    return Lists.transform(events, new FormToEventFunction());
  }

  private final class FormToEventFunction implements Function<IntermediateForm, ServerEvent> {
    @Override
    public ServerEvent apply(final IntermediateForm form) {
      if (form.objectId != null) {
        final byte[] value = oidToValueMap.get(form.objectId);
        if (value != null) {
          form.target.setValue(value);
        }
      }
      return form.target;
    }
  }

  private static final class IntermediateForm {
    private final VersionedServerEvent target;
    private final ObjectID objectId;

    private IntermediateForm(final VersionedServerEvent target, final ObjectID objectId) {
      this.target = target;
      this.objectId = objectId;
    }
  }

}
