/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import gnu.trove.TLongObjectHashMap;

public interface StringIndexPersistor {
  public TLongObjectHashMap loadMappingsInto(TLongObjectHashMap target);
  public void saveMapping(long index, String string);
}
