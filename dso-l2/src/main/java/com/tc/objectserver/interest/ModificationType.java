/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

/**
 * @author Eugene Shelestovich
 */
public enum ModificationType {
  //TODO: CLEAR ?
  PUT,
  UPDATE,
  REMOVE,
  EVICT,
  EXPIRE
}
