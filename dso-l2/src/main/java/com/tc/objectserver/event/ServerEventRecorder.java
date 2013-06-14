/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.event;

import com.tc.object.ObjectID;
import com.tc.objectserver.impl.SamplingType;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;

import java.util.List;

/**
 * Records all cache modifications and maps keys to actual values
 * omiting intermediate object id.
 *
 * @author Eugene Shelestovich
 */
public interface ServerEventRecorder {

  void recordEvent(ServerEventType type, Object key, ObjectID objectId,
                   String cacheName);

  void recordEvent(ServerEventType type, Object key, ObjectID objectId,
                   long version, String cacheName);

  void recordEventValue(ObjectID objectId, byte[] value);

  void reconsiderRemovals(SamplingType samplingType);

  List<ServerEvent> getEvents();
}
