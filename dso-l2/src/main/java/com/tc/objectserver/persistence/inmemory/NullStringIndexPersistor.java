/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.inmemory;

import com.tc.objectserver.persistence.api.StringIndexPersistor;

import java.util.Map;

public class NullStringIndexPersistor implements StringIndexPersistor {

  @Override
  public Map<Long, Object> loadMappingsInto(Map<Long, Object> target) {
    return target;
  }

  @Override
  public void saveMapping(long index, String string) {
    return;
  }

}
