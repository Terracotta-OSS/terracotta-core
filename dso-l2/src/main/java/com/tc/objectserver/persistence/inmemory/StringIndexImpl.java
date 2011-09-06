/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.inmemory;

import com.tc.io.serializer.api.StringIndex;
import com.tc.objectserver.persistence.api.StringIndexPersistor;

import gnu.trove.TLongObjectHashMap;
import gnu.trove.TLongObjectIterator;
import gnu.trove.TObjectLongHashMap;

public class StringIndexImpl implements StringIndex {
  private static final int           NULL = 0;
  private final StringIndexPersistor persistor;
  private final TLongObjectHashMap   indexToString;
  private final TObjectLongHashMap   stringToIndex;
  private long                       counter;

  public StringIndexImpl(StringIndexPersistor persistor) {
    this(persistor, 10);
  }

  public StringIndexImpl(StringIndexPersistor persistor, int initialCapacity) {
    this.persistor = persistor;

    this.indexToString = new TLongObjectHashMap(initialCapacity);
    this.stringToIndex = new TObjectLongHashMap(initialCapacity);

    for (TLongObjectIterator i = persistor.loadMappingsInto(indexToString).iterator(); i.hasNext();) {
      i.advance();
      long index = i.key();
      if (index > counter) counter = index;
      this.stringToIndex.put(i.value(), index);
    }
  }

  public synchronized long getOrCreateIndexFor(String string) {
    if (string == null) return NULL;
    long rv = stringToIndex.get(string);
    if (rv < 1) {
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
    for (TLongObjectIterator i = indexToString.iterator(); i.hasNext();) {
      i.advance();
      rv.append(i.key() + "=>" + i.value());
      if (i.hasNext()) rv.append(", ");
    }
    rv.append("]");
    return rv.toString();
  }
  
  // For Testing
  public TObjectLongHashMap getString2LongMappings() {
    return stringToIndex;
  }

}
