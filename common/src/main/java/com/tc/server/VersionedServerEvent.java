/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.server;

/**
 * @author Eugene Shelestovich
 */
public interface VersionedServerEvent extends ServerEvent {

  long DEFAULT_VERSION = -1L;

  void setValue(byte[] value);

  long getVersion();
}
