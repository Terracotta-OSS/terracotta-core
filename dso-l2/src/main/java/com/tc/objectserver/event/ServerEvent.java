/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.event;

import com.tc.object.ServerEventType;

/**
 * Server event.
 * TODO: add versioning to each event
 *
 * @author Eugene Shelestovich
 * @see ServerEventListener
 */
public interface ServerEvent {

  String getCacheName();

  ServerEventType getType();

  void setType(ServerEventType type);

  Object getKey();

  byte[] getValue();

  void setValue(byte[] value);
}
