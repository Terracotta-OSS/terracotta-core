/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import com.tc.exception.ImplementMe;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.util.ToggleableStrongReference;
import com.tc.util.concurrent.ThreadUtil;

import gnu.trove.TLinkable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import junit.framework.TestCase;

public class ArrayManagerTest extends TestCase {

  private final Map         registerdPairs = new IdentityHashMap();
  private static final List errors         = new ArrayList();
  private Object[]          arrays;

  @Override
  protected void setUp() throws Exception {
    for (int i = 0; i < 100; i++) {
      final TCObject tco = new FakeTCObject();
      final Object array = new Object[] {};
      this.registerdPairs.put(array, tco); // do this before register to always have strong ref
      ArrayManager.register(array, tco);
    }

    this.arrays = this.registerdPairs.keySet().toArray();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    synchronized (errors) {
      if (errors.size() > 0) {
        errors.clear();
        fail();
      }
    }
  }

  static void addError(final Throwable t) {
    t.printStackTrace();
    synchronized (errors) {
      errors.add(t);
    }
  }

  public void testCache() {
    final Random r = new Random();
    for (int i = 0; i < 100000; i++) {
      final Object array = this.arrays[r.nextInt(this.arrays.length)];
      final TCObject expected = (TCObject) this.registerdPairs.get(array);
      assertEquals(expected, ArrayManager.getObject(array));
    }
  }

  public void testReplaceCachedNegative() {
    final List refs = new ArrayList();
    for (int i = 0; i < 50000; i++) {
      final Object array = new Object[] {};
      refs.add(array);
      assertNull(ArrayManager.getObject(array));
      final TCObject tco = new FakeTCObject();
      ArrayManager.register(array, tco);
      assertEquals(tco, ArrayManager.getObject(array));
      assertEquals(tco, ArrayManager.getObject(array)); // do it twice
    }
  }

  public void testThreadsNoGC() throws Exception {
    testThreads(false);
  }

  public void testThreads() throws Exception {
    testThreads(true);
  }

  private void testThreads(final boolean withGC) throws Exception {
    final AddNew addNew = new AddNew();
    final Thread adder = new Thread(addNew, "Adder");

    final Query[] query = new Query[2];
    for (int i = 0; i < query.length; i++) {
      query[i] = new Query(this.arrays, this.registerdPairs, withGC);
    }
    final Thread[] queries = new Thread[query.length];
    for (int i = 0; i < queries.length; i++) {
      queries[i] = new Thread(query[i]);
      queries[i].setName("Query #" + i);
      queries[i].start();
    }

    adder.start();

    ThreadUtil.reallySleep(30000);

    addNew.stop();
    adder.join();

    for (int i = 0; i < queries.length; i++) {
      query[i].stop();
    }
    for (final Thread querie : queries) {
      querie.join();
    }

  }

  private static abstract class Base implements Runnable {
    private volatile boolean stop  = false;
    private int              count = 0;

    public void run() {
      try {
        while (!this.stop) {
          work();
          this.count++;
        }
      } catch (final Throwable t) {
        addError(t);
      } finally {
        System.err.println(Thread.currentThread().getName() + " made " + this.count + " loops");
      }
    }

    void stop() {
      this.stop = true;
    }

    abstract void work() throws Throwable;
  }

  private static class Query extends Base {
    private final Random   r = new Random();
    private final Map      pairs;
    private final Object[] arrays;
    private final boolean  withGC;

    Query(final Object[] arrays, final Map pairs, final boolean withGC) {
      this.arrays = arrays;
      this.pairs = pairs;
      this.withGC = withGC;
    }

    @Override
    void work() throws Throwable {
      if (this.r.nextBoolean()) {
        final Object array = this.arrays[this.r.nextInt(this.arrays.length)];
        final TCObject expect = (TCObject) this.pairs.get(array);
        if (expect != ArrayManager.getObject(array)) { throw new AssertionError("wrong mapping returned"); }
      } else {
        if (ArrayManager.getObject(new Object[] {}) != null) { throw new AssertionError(
                                                                                        "found object for brand new array"); }
      }

      if (this.withGC && (System.currentTimeMillis() % 255) == 0) {
        System.out.println(Thread.currentThread().getName() + " doing DGC");
        System.gc();
      }

    }
  }

  private class AddNew extends Base {
    @Override
    void work() {
      final Object newArray = new Object[] {};
      ArrayManager.getObject(newArray);
      ArrayManager.register(newArray, new FakeTCObject());

      for (int i = 0; i < 10; i++) {
        // this strange test should make it impossible for the runtime to eliminate this code
        if (hashCode() == System.currentTimeMillis()) {
          System.out.println("BONK -- This is harmless ;-)");
        }
      }

    }
  }

  private static class FakeTCObject implements TCObject {

    public boolean autoLockingDisabled() {
      throw new ImplementMe();
    }

    public void booleanFieldChanged(final String classname, final String fieldname, final boolean newValue,
                                    final int index) {
      throw new ImplementMe();
    }

    public void byteFieldChanged(final String classname, final String fieldname, final byte newValue, final int index) {
      throw new ImplementMe();
    }

    public void charFieldChanged(final String classname, final String fieldname, final char newValue, final int index) {
      throw new ImplementMe();
    }

    public void clearReference(final String fieldName) {
      throw new ImplementMe();
    }

    public int clearReferences(final int toClear) {
      throw new ImplementMe();
    }

    public void disableAutoLocking() {
      throw new ImplementMe();
    }

    public void doubleFieldChanged(final String classname, final String fieldname, final double newValue,
                                   final int index) {
      throw new ImplementMe();
    }

    public void floatFieldChanged(final String classname, final String fieldname, final float newValue, final int index) {
      throw new ImplementMe();
    }

    public String getFieldNameByOffset(final long fieldOffset) {
      throw new ImplementMe();
    }

    public TLinkable getNext() {
      throw new ImplementMe();
    }

    public ObjectID getObjectID() {
      throw new ImplementMe();
    }

    public Object getPeerObject() {
      throw new ImplementMe();
    }

    public TLinkable getPrevious() {
      throw new ImplementMe();
    }

    public Object getResolveLock() {
      throw new ImplementMe();
    }

    public TCClass getTCClass() {
      throw new ImplementMe();
    }

    public long getVersion() {
      throw new ImplementMe();
    }

    public void hydrate(final DNA from, final boolean force, WeakReference peer) throws DNAException {
      throw new ImplementMe();
    }

    public void intFieldChanged(final String classname, final String fieldname, final int newValue, final int index) {
      throw new ImplementMe();
    }

    public boolean isNew() {
      throw new ImplementMe();
    }

    public boolean isShared() {
      throw new ImplementMe();
    }

    public void logicalInvoke(final int method, final String methodSignature, final Object[] params) {
      throw new ImplementMe();
    }

    public void longFieldChanged(final String classname, final String fieldname, final long newValue, final int index) {
      throw new ImplementMe();
    }

    public void objectFieldChanged(final String classname, final String fieldname, final Object newValue,
                                   final int index) {
      throw new ImplementMe();
    }

    public void objectFieldChangedByOffset(final String classname, final long fieldOffset, final Object newValue,
                                           final int index) {
      throw new ImplementMe();
    }

    public void resolveAllReferences() {
      throw new ImplementMe();
    }

    public void resolveArrayReference(final int index) {
      throw new ImplementMe();
    }

    public void resolveReference(final String fieldName) {
      throw new ImplementMe();
    }

    public void setNext(final TLinkable link) {
      throw new ImplementMe();
    }

    public void setPrevious(final TLinkable link) {
      throw new ImplementMe();
    }

    public ObjectID setReference(final String fieldName, final ObjectID id) {
      throw new ImplementMe();
    }

    public void setArrayReference(final int index, final ObjectID id) {
      throw new ImplementMe();
    }

    public void setValue(final String fieldName, final Object obj) {
      throw new ImplementMe();
    }

    public void setVersion(final long version) {
      throw new ImplementMe();
    }

    public void shortFieldChanged(final String classname, final String fieldname, final short newValue, final int index) {
      throw new ImplementMe();
    }

    public boolean canEvict() {
      throw new ImplementMe();
    }

    public void clearAccessed() {
      throw new ImplementMe();
    }

    public void markAccessed() {
      throw new ImplementMe();
    }

    public boolean recentlyAccessed() {
      throw new ImplementMe();
    }

    public void objectArrayChanged(final int startPos, final Object[] array, final int length) {
      throw new ImplementMe();
    }

    public void primitiveArrayChanged(final int startPos, final Object array, final int length) {
      throw new ImplementMe();
    }

    public int accessCount(final int factor) {
      throw new ImplementMe();
    }

    public void literalValueChanged(final Object newValue, final Object oldValue) {
      throw new ImplementMe();
    }

    public void setLiteralValue(final Object newValue) {
      throw new ImplementMe();
    }

    public boolean isFieldPortableByOffset(final long fieldOffset) {
      throw new ImplementMe();
    }

    public ToggleableStrongReference getOrCreateToggleRef() {
      throw new ImplementMe();
    }

    public void setNotNew() {
      throw new ImplementMe();
    }

    public void dehydrate(final DNAWriter writer) {
      throw new ImplementMe();
    }

    public void unresolveReference(final String fieldName) {
      throw new ImplementMe();
    }

    public boolean isCacheManaged() {
      throw new ImplementMe();
    }
  }

}
