/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.collections.servermap.api.ehcacheimpl;

import java.util.ArrayList;
import java.util.List;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.terracotta.InternalEhcache;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Matchers;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author mscott
 */
public class OnlineEhcacheSMLocalStoreTest {
  
  public OnlineEhcacheSMLocalStoreTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

  /**
   * Test of get method, of class OnlineEhcacheSMLocalStore.
   */
  @Test
  public void testGet() {
    Object key = "test";
    Object value = "value";
    Element old = new Element(key,value);
    
    InternalEhcache base = mock(InternalEhcache.class);
    when(base.get(Matchers.any())).thenReturn(old);
    
    CacheConfiguration config = mock(CacheConfiguration.class);
    when(config.isOverflowToOffHeap()).thenReturn(false);
    when(base.getCacheConfiguration()).thenReturn(config);
    
    OnlineEhcacheSMLocalStore instance = new OnlineEhcacheSMLocalStore(base);
    Object result = instance.get(key);
    assertEquals(old.getObjectValue(), result);
    
    verify(base).get(Matchers.any());
  }

  /**
   * Test of getKeys method, of class OnlineEhcacheSMLocalStore.
   */
  @Test
  public void testGetKeys() {
    List<Object> keys = new ArrayList<Object>(5);
    for (int x=0;x<5;x++) {
      keys.add(Integer.toString(x));
    }
    
    InternalEhcache base = mock(InternalEhcache.class);
    when(base.getKeys()).thenReturn(keys);
    
    CacheConfiguration config = mock(CacheConfiguration.class);
    when(config.isOverflowToOffHeap()).thenReturn(false);
    when(base.getCacheConfiguration()).thenReturn(config);
    
    OnlineEhcacheSMLocalStore instance = new OnlineEhcacheSMLocalStore(base);
    instance.getKeys();
    
    verify(base).getKeys();
  }

  /**
   * Test of put method, of class OnlineEhcacheSMLocalStore.
   */
  @Test
  public void testPut() throws Exception {
    Object key = "test";
    Object value = "value";
    Element old = new Element("test","old");
    
    InternalEhcache base = mock(InternalEhcache.class);
    when(base.removeAndReturnElement(Matchers.eq(key))).thenReturn(old);
    when(base.putIfAbsent(Matchers.any(Element.class))).thenReturn(old);
    when(base.replace(Matchers.eq(old), Matchers.any(Element.class))).thenReturn(true);
    
    CacheConfiguration config = mock(CacheConfiguration.class);
    when(config.isOverflowToOffHeap()).thenReturn(false);
    when(base.getCacheConfiguration()).thenReturn(config);
    
    OnlineEhcacheSMLocalStore instance = new OnlineEhcacheSMLocalStore(base);
    Object result = instance.put(key, value);
    assertEquals(old.getObjectValue(), result);
    
//    verify(base).putIfAbsent(Matchers.any(Element.class));
//    verify(base).replace(Matchers.eq(old), Matchers.any(Element.class));
  }

  /**
   * Test of remove method, of class OnlineEhcacheSMLocalStore.
   */
  @Test
  public void testRemove_Object() {
    Object key = "test";
    Object value = "value";
    Element old = new Element(key,value);
    
    InternalEhcache base = mock(InternalEhcache.class);
    when(base.removeAndReturnElement(Matchers.any())).thenReturn(old);
    
    CacheConfiguration config = mock(CacheConfiguration.class);
    when(config.isOverflowToOffHeap()).thenReturn(false);
    when(base.getCacheConfiguration()).thenReturn(config);
    
    OnlineEhcacheSMLocalStore instance = new OnlineEhcacheSMLocalStore(base);
    Object result = instance.remove(key);
    assertEquals(old.getObjectValue(), result);
    
    verify(base).removeAndReturnElement(Matchers.any());
  }

  /**
   * Test of remove method, of class OnlineEhcacheSMLocalStore.
   */
  @Test
  public void testRemove_Object_Object() {
    Object key = "test";
    Object value = "value";
    Element old = new Element(key,value);
    
    InternalEhcache base = mock(InternalEhcache.class);
    when(base.get(Matchers.any())).thenReturn(old);
    when(base.removeElement(Matchers.eq(old))).thenReturn(true);
    
    CacheConfiguration config = mock(CacheConfiguration.class);
    when(config.isOverflowToOffHeap()).thenReturn(false);
    when(base.getCacheConfiguration()).thenReturn(config);
    
    OnlineEhcacheSMLocalStore instance = new OnlineEhcacheSMLocalStore(base);
    Object result = instance.remove(key, value);
    assertEquals(old.getObjectValue(), result);
    
    verify(base).get(Matchers.any());
    verify(base).removeElement(Matchers.eq(old));
  }

  /**
   * Test of clear method, of class OnlineEhcacheSMLocalStore.
   */
  @Test
  public void testClear() {
    Object key = "test";
    Object value = "value";
    Element old = new Element(key,value);
    
    InternalEhcache base = mock(InternalEhcache.class);
    
    CacheConfiguration config = mock(CacheConfiguration.class);
    when(config.isOverflowToOffHeap()).thenReturn(false);
    when(base.getCacheConfiguration()).thenReturn(config);
    
    OnlineEhcacheSMLocalStore instance = new OnlineEhcacheSMLocalStore(base);
    instance.clear();
    
    verify(base).removeAll();
  }

  /**
   * Test of cleanLocalState method, of class OnlineEhcacheSMLocalStore.
   */
  @Test
  public void testCleanLocalState() {
    Object key = "test";
    Object value = "value";
    Element old = new Element(key,value);
    
    InternalEhcache base = mock(InternalEhcache.class);
    
    CacheConfiguration config = mock(CacheConfiguration.class);
    when(config.isOverflowToOffHeap()).thenReturn(false);
    when(base.getCacheConfiguration()).thenReturn(config);
    
    OnlineEhcacheSMLocalStore instance = new OnlineEhcacheSMLocalStore(base);
    instance.cleanLocalState();
    
    verify(base).removeAll(Matchers.eq(true));
  }
  
}
