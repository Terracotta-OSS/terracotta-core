package com.tc.server;

/**
 * Type of server cache event.
 *
 * @author Eugene Shelestovich
 */
public enum ServerEventType {
  /**
   * All 'puts' including replicated events from different regions.
   */
  PUT,
  /**
   * Local region 'puts' initiated by clients.
   */
  PUT_LOCAL,
  /**
   * All 'removes' including replicated events from different regions.
   */
  REMOVE,
  /**
   * Local region 'removals' initiated by clients.
   */
  REMOVE_LOCAL,

  EVICT,

  EXPIRE
}
