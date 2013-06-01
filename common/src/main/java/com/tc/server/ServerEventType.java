package com.tc.server;

/**
 * Type of server cache event.
 *
 * @author Eugene Shelestovich
 */
public enum ServerEventType {
  PUT,
  REMOVE,
  EVICT,
  EXPIRE
}
