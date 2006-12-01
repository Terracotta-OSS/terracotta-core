/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.objectserver.persistence.api.StringIndexPersistor;

import gnu.trove.TLongObjectHashMap;

public class NullStringIndexPersistor implements StringIndexPersistor {

  public TLongObjectHashMap loadMappingsInto(TLongObjectHashMap target) {
    return target;
  }

  public void saveMapping(long index, String string) {
    return;
  }

}
