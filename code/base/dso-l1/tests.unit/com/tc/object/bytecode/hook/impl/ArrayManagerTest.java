/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import com.tc.util.concurrent.ThreadUtil;

import gnu.trove.TLinkable;

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

  protected void setUp() throws Exception {
    for (int i = 0; i < 100; i++) {
      TCObject tco = new FakeTCObject();
      Object array = new Object[] {};
      registerdPairs.put(array, tco); // do this before register to always have strong ref
      ArrayManager.register(array, tco);
    }

    arrays = registerdPairs.keySet().toArray();
  }

  protected void tearDown() throws Exception {
    super.tearDown();

    synchronized (errors) {
      if (errors.size() > 0) {
        errors.clear();
        fail();
      }
    }
  }

  static void addError(Throwable t) {
    t.printStackTrace();
    synchronized (errors) {
      errors.add(t);
    }
  }

  public void testCache() {
    Random r = new Random();
    for (int i = 0; i < 100000; i++) {
      Object array = arrays[r.nextInt(arrays.length)];
      TCObject expected = (TCObject) registerdPairs.get(array);
      assertEquals(expected, ArrayManager.getObject(array));
    }
  }

  public void testReplaceCachedNegative() {
    List refs = new ArrayList();
    for (int i = 0; i < 50000; i++) {
      Object array = new Object[] {};
      refs.add(array);
      assertNull(ArrayManager.getObject(array));
      TCObject tco = new FakeTCObject();
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

  private void testThreads(boolean withGC) throws Exception {
    AddNew addNew = new AddNew();
    Thread adder = new Thread(addNew, "Adder");

    Query[] query = new Query[2];
    for (int i = 0; i < query.length; i++) {
      query[i] = new Query(arrays, registerdPairs, withGC);
    }
    Thread[] queries = new Thread[query.length];
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
    for (int i = 0; i < queries.length; i++) {
      queries[i].join();
    }

  }

  private static abstract class Base implements Runnable {
    private volatile boolean stop  = false;
    private int              count = 0;

    public void run() {
      try {
        while (!stop) {
          work();
          count++;
        }
      } catch (Throwable t) {
        addError(t);
      } finally {
        System.err.println(Thread.currentThread().getName() + " made " + count + " loops");
      }
    }

    void stop() {
      stop = true;
    }

    abstract void work() throws Throwable;
  }

  private static class Query extends Base {
    private final Random   r = new Random();
    private final Map      pairs;
    private final Object[] arrays;
    private final boolean  withGC;

    Query(Object[] arrays, Map pairs, boolean withGC) {
      this.arrays = arrays;
      this.pairs = pairs;
      this.withGC = withGC;
    }

    void work() throws Throwable {
      if (r.nextBoolean()) {
        Object array = arrays[r.nextInt(arrays.length)];
        TCObject expect = (TCObject) pairs.get(array);
        if (expect != ArrayManager.getObject(array)) { throw new AssertionError("wrong mapping returned"); }
      } else {
        if (ArrayManager.getObject(new Object[] {}) != null) { throw new AssertionError(
                                                                                        "found object for brand new array"); }
      }

      if (withGC && (System.currentTimeMillis() % 255) == 0) {
        System.out.println(Thread.currentThread().getName() + " doing GC");
        System.gc();
      }

    }
  }

  private class AddNew extends Base {
    void work() {
      Object newArray = new Object[] {};
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

    public void booleanFieldChanged(String classname, String fieldname, boolean newValue, int index) {
      throw new ImplementMe();
    }

    public void byteFieldChanged(String classname, String fieldname, byte newValue, int index) {
      throw new ImplementMe();
    }

    public void charFieldChanged(String classname, String fieldname, char newValue, int index) {
      throw new ImplementMe();
    }

    public void clearReference(String fieldName) {
      throw new ImplementMe();
    }

    public int clearReferences(int toClear) {
      throw new ImplementMe();
    }

    public boolean dehydrateIfNew(DNAWriter writer) throws DNAException {
      throw new ImplementMe();
    }

    public void disableAutoLocking() {
      throw new ImplementMe();
    }

    public void doubleFieldChanged(String classname, String fieldname, double newValue, int index) {
      throw new ImplementMe();
    }

    public void floatFieldChanged(String classname, String fieldname, float newValue, int index) {
      throw new ImplementMe();
    }

    public String getFieldNameByOffset(long fieldOffset) {
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

    public void hydrate(DNA from, boolean force) throws DNAException {
      throw new ImplementMe();
    }

    public void intFieldChanged(String classname, String fieldname, int newValue, int index) {
      throw new ImplementMe();
    }

    public boolean isNew() {
      throw new ImplementMe();
    }

    public boolean isShared() {
      throw new ImplementMe();
    }

    public void logicalInvoke(int method, String methodSignature, Object[] params) {
      throw new ImplementMe();
    }

    public void longFieldChanged(String classname, String fieldname, long newValue, int index) {
      throw new ImplementMe();
    }

    public void objectFieldChanged(String classname, String fieldname, Object newValue, int index) {
      throw new ImplementMe();
    }

    public void objectFieldChangedByOffset(String classname, long fieldOffset, Object newValue, int index) {
      throw new ImplementMe();
    }

    public void resolveAllReferences() {
      throw new ImplementMe();
    }

    public void resolveArrayReference(int index) {
      throw new ImplementMe();
    }

    public void resolveReference(String fieldName) {
      throw new ImplementMe();
    }

    public void setIsNew() {
      throw new ImplementMe();
    }

    public void setNext(TLinkable link) {
      throw new ImplementMe();
    }

    public void setPrevious(TLinkable link) {
      throw new ImplementMe();
    }

    public ObjectID setReference(String fieldName, ObjectID id) {
      throw new ImplementMe();
    }

    public void setArrayReference(int index, ObjectID id) {
      throw new ImplementMe();
    }

    public void setValue(String fieldName, Object obj) {
      throw new ImplementMe();
    }

    public void setVersion(long version) {
      throw new ImplementMe();
    }

    public void shortFieldChanged(String classname, String fieldname, short newValue, int index) {
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

    public void objectArrayChanged(int startPos, Object[] array, int length) {
      throw new ImplementMe();
    }

    public void primitiveArrayChanged(int startPos, Object array, int length) {
      throw new ImplementMe();
    }

    public int accessCount(int factor) {
      throw new ImplementMe();
    }

    public void literalValueChanged(Object newValue, Object oldValue) {
      throw new ImplementMe();
    }

    public void setLiteralValue(Object newValue) {
      throw new ImplementMe();
    }

    public ArrayIndexOutOfBoundsException checkArrayIndex(int index) {
      throw new ImplementMe();
    }

  }

}
