/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import gnu.trove.TLongObjectHashMap;

public interface StringIndexPersistor {
  public TLongObjectHashMap loadMappingsInto(TLongObjectHashMap target);
  public void saveMapping(long index, String string);
}
