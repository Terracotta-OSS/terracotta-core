/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.server;

/**
 * Server event.
 *
 * @author Eugene Shelestovich
 */
public interface ServerEvent {

  String getCacheName();

  ServerEventType getType();

  void setType(ServerEventType type);

  Object getKey();

  byte[] getValue();

}
