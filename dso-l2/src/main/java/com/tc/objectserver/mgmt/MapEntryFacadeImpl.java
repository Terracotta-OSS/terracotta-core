/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.mgmt;

import java.io.Serializable;

public class MapEntryFacadeImpl implements MapEntryFacade, Serializable {

  private final Object value;
  private final Object key;

  public MapEntryFacadeImpl(Object key, Object value) {
    this.key = key;
    this.value = value;
  }

  public Object getKey() {
    return this.key;
  }

  public Object getValue() {
    return this.value;
  }

}
