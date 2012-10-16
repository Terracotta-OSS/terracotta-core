/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.inmemory;

import com.tc.io.serializer.api.StringIndex;
import com.tc.objectserver.persistence.api.StringIndexPersistor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class StringIndexImpl implements StringIndex {
  private static final int           NULL = 0;
  private final StringIndexPersistor persistor;
  private final Map<Long, String>    indexToString;
  private final Map<String, Long>    stringToIndex;
  private long                       counter;

  public StringIndexImpl(StringIndexPersistor persistor) {
    this(persistor, 10);
  }

  public StringIndexImpl(StringIndexPersistor persistor, int initialCapacity) {
    this.persistor = persistor;

    this.indexToString = new HashMap<Long, String>(initialCapacity);
    this.stringToIndex = new HashMap<String, Long>(initialCapacity);

    for (Map.Entry<Long, String> e : persistor.loadMappingsInto(indexToString).entrySet()) {
      long index = e.getKey();
      if (index > counter) counter = index;
      this.stringToIndex.put(e.getValue(), index);
    }
  }

  public synchronized long getOrCreateIndexFor(String string) {
    if (string == null) return NULL;
    Long rv = stringToIndex.get(string);
    if (rv == null) {
      rv = ++counter;
      indexToString.put(rv, string);
      stringToIndex.put(string, rv);
      persistor.saveMapping(rv, string);
    }
    return rv;
  }

  public synchronized String getStringFor(long index) {
    if (index == NULL) return null;
    String rv = (String) indexToString.get(index);
    if (rv == null) throw new AssertionError("Unknown index: " + index);
    return rv;
  }

  public synchronized String toString() {
    StringBuffer rv = new StringBuffer();
    rv.append("[");
    for (Iterator<Map.Entry<Long, String>> i = indexToString.entrySet().iterator(); i.hasNext();) {
      Map.Entry<Long, String> e = i.next();
      rv.append(e.getKey() + "=>" + e.getValue());
      if (i.hasNext()) rv.append(", ");
    }
    rv.append("]");
    return rv.toString();
  }
  
  // For Testing
  public Map<String, Long> getString2LongMappings() {
    return stringToIndex;
  }

}
