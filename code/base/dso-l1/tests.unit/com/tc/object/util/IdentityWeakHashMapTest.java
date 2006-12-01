/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.util;

import java.util.WeakHashMap;

import junit.framework.TestCase;

/**
 * @author steve
 */
public class IdentityWeakHashMapTest extends TestCase {
  public void tests() throws Exception {
    examineIdentityWeakHashMap();
    examineWeakHashMap();
  }
  
  private void examineIdentityWeakHashMap() {
    IdentityWeakHashMap map = new IdentityWeakHashMap();
    TestObj k1 = new TestObj();
    TestObj k2 = new TestObj();
    map.put(k1, new Integer(1));
    assertTrue(map.size() == 1);
    assertTrue(map.containsKey(k1));
    assertFalse(map.containsKey(new TestObj()));
    map.put(k2, new Integer(2));
    assertTrue(map.size() == 2);
    assertTrue(map.containsKey(k1));
    assertTrue(map.containsKey(k2));
    map.put(k1, new Integer(3));
    assertTrue(map.size()==2);
    assertTrue(map.containsKey(k1));
    assertTrue(map.get(k1).equals(new Integer(3)));
    
    TestObj[] objects = new TestObj[1000];
    for(int i=0;i<objects.length;i++){
      objects[i] = new TestObj();
      map.put(objects[i], new Long(i));
    }
    
    for(int i=0;i<objects.length;i++){
      map.put(objects[i], new Long(i));
    }
    
    for(int i=0;i<objects.length;i++){
      map.remove(objects[i]);
    }
    System.out.println("size:"+map.size());
    assertTrue(map.size()==2);
  }
  
  private void examineWeakHashMap() {
    WeakHashMap map = new WeakHashMap();
    TestObj k1 = new TestObj();
    TestObj k2 = new TestObj();
    map.put(k1, new Integer(1));
    assertTrue(map.size() == 1);
    assertTrue(map.containsKey(k1));
    assertTrue(map.containsKey(new TestObj()));
    map.put(k2, new Integer(2));
    assertFalse(map.size() == 2);
    assertTrue(map.size() == 1);
    assertTrue(map.containsKey(k1));
    assertTrue(map.containsKey(k2));
    map.put(k1, new Integer(3));
    assertTrue(map.size()==1);
    assertTrue(map.containsKey(k1));
    assertTrue(map.get(k1).equals(new Integer(3)));
    
    TestObj[] objects = new TestObj[1000];
    for(int i=0;i<objects.length;i++){
      objects[i] = new TestObj();
      map.put(objects[i], new Long(i));
    }
    
    for(int i=0;i<objects.length;i++){
      map.put(objects[i], new Long(i));
    }
    
    map.remove(objects[0]);
    System.out.println("size:"+map.size());
    assertTrue(map.size()==0);
  }
  
  private class TestObj {
    public int hashCode(){
      return 10;
    }
    
    public boolean equals(Object obj){
      return true;
    }
  }
}
