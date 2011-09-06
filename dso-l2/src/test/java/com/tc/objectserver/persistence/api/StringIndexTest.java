/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import com.tc.io.serializer.api.StringIndex;
import com.tc.objectserver.persistence.inmemory.StringIndexImpl;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import gnu.trove.TLongObjectHashMap;
import gnu.trove.TLongObjectIterator;
import junit.framework.TestCase;

public class StringIndexTest extends TestCase {

  private TestStringIndexPersistor persistor;
  private StringIndex index;
  
  public void setUp() throws Exception {
    persistor = new TestStringIndexPersistor();
  }
  
  public void test() throws Exception {
    index = new StringIndexImpl(persistor);
    // make sure it loads all the data on construction
    assertNotNull(persistor.loadCalls.poll(0));

    // make sure index 0 returns null;
    assertNull(index.getStringFor(0));
    
    // make sure it assigns the index as expected
    String testString = ":SLDKFJSD";
    assertEquals(1, index.getOrCreateIndexFor(testString));
    
    // make sure it save the mapping as expected
    assertEquals(testString, persistor.target.get(1));
    
    // clear the map
    persistor.target.clear();
    
    // load up the persistor
    int max = 100;
    for (int i=1; i<max; i++) {
      persistor.target.put(i, "" + i);
    }
    index = new StringIndexImpl(persistor);
    
    // make sure the mappings are loaded as expected
    for (int i=1; i<max; i++) {
      String string = "" + i;
      assertEquals(string, index.getStringFor(i));
      assertEquals(i, index.getOrCreateIndexFor(string));
    }
    
    // make sure new index assignments start at the max
    testString = "lksdfkljdfkjl";
    assertEquals(max, index.getOrCreateIndexFor(testString));
    
    assertNull(index.getStringFor(0));
  }
  
  private static final class TestStringIndexPersistor implements StringIndexPersistor {

    public TLongObjectHashMap target = new TLongObjectHashMap();
    public NoExceptionLinkedQueue loadCalls = new NoExceptionLinkedQueue();
    
    public TLongObjectHashMap loadMappingsInto(TLongObjectHashMap theTarget) {
      loadCalls.put(theTarget);
      for (TLongObjectIterator i = target.iterator(); i.hasNext();) {
        i.advance();
        theTarget.put(i.key(), i.value());
      }
      return theTarget;
    }

    public void saveMapping(long index, String string) {
      target.put(index, string);
    }
    
  }
}
