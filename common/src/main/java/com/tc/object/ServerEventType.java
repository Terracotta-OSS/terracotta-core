package com.tc.object;

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
