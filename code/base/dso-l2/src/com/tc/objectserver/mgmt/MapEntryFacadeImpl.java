/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
