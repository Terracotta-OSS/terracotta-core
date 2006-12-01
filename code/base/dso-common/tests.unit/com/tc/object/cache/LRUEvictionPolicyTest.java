/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.cache;

import gnu.trove.TLinkable;

import java.util.Collection;
import java.util.Iterator;

import com.tc.object.ObjectID;
import com.tc.test.TCTestCase;

/**
 * 
 */
public class LRUEvictionPolicyTest extends TCTestCase {

  public EvictionPolicy createNewCache(int size) {
    return new LRUEvictionPolicy(size);
  }

  public void tests() throws Exception {
    int cacheSize = 10;
    EvictionPolicy slc = createNewCache(cacheSize);
    Cacheable[] cacheables = new Cacheable[cacheSize];
    for (int i = 0; i < cacheSize; i++) {
      cacheables[i] = new TestCacheable(new ObjectID(i));
      boolean evict = slc.add(cacheables[i]);
      assertFalse(evict);
    }
    assertTrue(slc.add(new TestCacheable(new ObjectID(11))));
    Collection c = slc.getRemovalCandidates(-1);
    assertEquals(new ObjectID(0), ((Cacheable) (c.iterator().next())).getObjectID());
    removeAll(slc, c);

    slc.markReferenced(cacheables[1]);

    assertTrue(slc.add(new TestCacheable(new ObjectID(12))));
    c = slc.getRemovalCandidates(-1);
    assertEquals(1, c.size());
    assertTrue(c.iterator().next() == cacheables[2]);

    slc.remove(cacheables[3]);

    slc.add(new TestCacheable(new ObjectID(13)));
    c = slc.getRemovalCandidates(-1);
    assertTrue(c.iterator().next() == cacheables[4]);

    slc.add(new TestCacheable(new ObjectID(14)));
    c = slc.getRemovalCandidates(-1);

    assertEquals(1, c.size());
    assertTrue(c.contains(cacheables[5]));
    slc.remove(cacheables[5]);

    c = slc.getRemovalCandidates(-1);
    assertEquals(1, c.size());
    assertTrue(c.contains(cacheables[6]));
    slc.remove(cacheables[6]);

    // repopulate the cache with just items from 'cachables'
    for (int i = 0; i < cacheables.length; i++) {
      cacheables[i] = new TestCacheable(new ObjectID(100 + i));
      slc.add(cacheables[i]);
      c = slc.getRemovalCandidates(-1);
      removeAll(slc, c);
    }

    // go through the cache again and assert that the cache is back down to the proper size and
    // the LRU policy is still in effect.
    for (int i = 0; i < cacheables.length; i++) {
      slc.add(new TestCacheable(new ObjectID(200 + i)));
      c = slc.getRemovalCandidates(-1);
      assertEquals(1, c.size());
      Cacheable evicted = (Cacheable) c.iterator().next();
      assertTrue(evicted == cacheables[i]);
      removeAll(slc, c);
    }
  }

  protected void removeAll(EvictionPolicy slc, Collection c) {
    for (Iterator iter = c.iterator(); iter.hasNext();) {
      slc.remove((Cacheable) iter.next());
    }
  }

  public static class TestCacheable implements Cacheable {
    private ObjectID  id;
    private TLinkable next;
    private TLinkable previous;
    private int       accessed = 0;

    public TestCacheable(ObjectID id) {
      this.id = id;
    }

    public TestCacheable(ObjectID id, int accessed) {
      this.id = id;
      this.accessed = accessed;
    }

    public ObjectID getObjectID() {
      return id;
    }

    public void markAccessed() {
      this.accessed++;
    }

    public TLinkable getNext() {
      return next;
    }

    public TLinkable getPrevious() {
      return previous;
    }

    public void setNext(TLinkable next) {
      this.next = next;
    }

    public void setPrevious(TLinkable previous) {
      this.previous = previous;
    }

    public String toString() {
      return "TestCacheable[" + id + "]";
    }

    public void clearAccessed() {
      this.accessed = 0;

    }

    public boolean recentlyAccessed() {
      return (this.accessed > 0);
    }

    public boolean canEvict() {
      return true;
    }

    public int accessCount(int factor) {
      accessed = accessed / factor;
      return accessed;
    }
    
    public int accessCount() {
      return accessed;
    }
  }
}