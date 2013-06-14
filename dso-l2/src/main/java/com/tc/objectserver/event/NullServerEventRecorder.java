package com.tc.objectserver.event;

import com.tc.object.ObjectID;
import com.tc.objectserver.impl.SamplingType;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;

import java.util.Collections;
import java.util.List;

/**
 * @author Eugene Shelestovich
 */
public class NullServerEventRecorder implements ServerEventRecorder {

  @Override
  public void recordEvent(final ServerEventType type, final Object key, final ObjectID objectId, final String cacheName) {
  }

  @Override
  public void recordEvent(final ServerEventType type, final Object key, final ObjectID objectId, final long version, final String cacheName) {
  }

  @Override
  public void recordEventValue(final ObjectID objectId, final byte[] value) {
  }

  @Override
  public void reconsiderRemovals(final SamplingType samplingType) {
  }

  @Override
  public List<ServerEvent> getEvents() {
    return Collections.emptyList();
  }
}
