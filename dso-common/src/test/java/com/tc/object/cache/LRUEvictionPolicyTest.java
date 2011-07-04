/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.object.ObjectID;
import com.tc.test.TCTestCase;

import gnu.trove.TLinkable;

import java.util.Collection;
import java.util.Iterator;

/**
 * 
 */
public class LRUEvictionPolicyTest extends TCTestCase {

  public EvictionPolicy createNewCache(final int size) {
    return new LRUEvictionPolicy(size);
  }

  public void tests() throws Exception {
    final int cacheSize = 10;
    final EvictionPolicy slc = createNewCache(cacheSize);
    final Cacheable[] cacheables = new Cacheable[cacheSize];
    for (int i = 0; i < cacheSize; i++) {
      cacheables[i] = new TestCacheable(new ObjectID(i));
      final boolean evict = slc.add(cacheables[i]);
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
      final Cacheable evicted = (Cacheable) c.iterator().next();
      assertTrue(evicted == cacheables[i]);
      removeAll(slc, c);
    }
  }

  protected void removeAll(final EvictionPolicy slc, final Collection c) {
    for (final Iterator iter = c.iterator(); iter.hasNext();) {
      slc.remove((Cacheable) iter.next());
    }
  }

  public static class TestCacheable implements Cacheable {
    private final ObjectID id;
    private TLinkable      next;
    private TLinkable      previous;
    private int            accessed = 0;

    public TestCacheable(final ObjectID id) {
      this.id = id;
    }

    public TestCacheable(final ObjectID id, final int accessed) {
      this.id = id;
      this.accessed = accessed;
    }

    public ObjectID getObjectID() {
      return this.id;
    }

    public void markAccessed() {
      this.accessed++;
    }

    public TLinkable getNext() {
      return this.next;
    }

    public TLinkable getPrevious() {
      return this.previous;
    }

    public void setNext(final TLinkable next) {
      this.next = next;
    }

    public void setPrevious(final TLinkable previous) {
      this.previous = previous;
    }

    @Override
    public String toString() {
      return "TestCacheable[" + this.id + "]";
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

    public int accessCount(final int factor) {
      this.accessed = this.accessed / factor;
      return this.accessed;
    }

    public int accessCount() {
      return this.accessed;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj instanceof TestCacheable) {
        final TestCacheable t2 = (TestCacheable) obj;
        return getObjectID().equals(t2.getObjectID());
      }
      return false;
    }

    @Override
    public int hashCode() {
      return getObjectID().hashCode();
    }

    public boolean isCacheManaged() {
      return true;
    }
  }
}