/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.TestDNACursor;
import com.tc.object.TestDNAWriter;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.persistence.inmemory.InMemoryPersistor;
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public abstract class AbstractTestManagedObjectState extends TestCase {
  protected ObjectID                            objectID;
  protected ManagedObjectChangeListenerProvider listenerProvider;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.listenerProvider = new NullManagedObjectChangeListenerProvider();
    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(this.listenerProvider, new InMemoryPersistor());
    this.objectID = new ObjectID(2000);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    ManagedObjectStateFactory.disableSingleton(false);
    this.objectID = null;
    this.listenerProvider = null;
  }

  protected ManagedObjectState createManagedObjectState(final String className, final TestDNACursor cursor)
      throws Exception {
    return createManagedObjectState(className, cursor, ObjectID.NULL_ID);
  }

  protected ManagedObjectState createManagedObjectState(final String className, final TestDNACursor cursor,
                                                        final ObjectID parentID) throws Exception {
    final ManagedObjectState state = ManagedObjectStateFactory.getInstance().createState(new ObjectID(1), parentID,
                                                                                         className, cursor);
    return state;
  }

  public void basicTestUnit(final String className, final byte type, final TestDNACursor cursor, final int objCount)
      throws Exception {
    basicTestUnit(className, type, cursor, objCount, true);
  }

  public void basicTestUnit(final String className, final byte type, final TestDNACursor cursor, final int objCount,
                            final boolean verifyReadWrite) throws Exception {
    final ManagedObjectState state = createManagedObjectState(className, cursor);
    state.apply(this.objectID, cursor, new ApplyTransactionInfo());

    // API verification
    basicAPI(className, type, cursor, objCount, state);

    // dehydrate
    basicDehydrate(cursor, objCount, state);

    // writeTo, readFrom and equal
    if (verifyReadWrite) {
      basicReadWriteEqual(type, state);
    }
  }

  protected void basicAPI(final String className, final byte type, final TestDNACursor cursor, final int objCount,
                          final ManagedObjectState state) {
    Assert.assertEquals("BackReferences object size", objCount, state.getObjectReferences().size());
    Assert.assertTrue(state.getType() == type);
    Assert.assertTrue("ClassName:" + state.getClassName(), state.getClassName().equals(className));

  }

  protected void basicDehydrate(final TestDNACursor cursor, final int objCount, final ManagedObjectState state) {
    final TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(this.objectID, dnaWriter, DNAType.L1_FAULT);
    cursor.reset();
    cursor.next();
    while (cursor.next()) {
      final Object action = cursor.getAction();
      Assert.assertTrue(dnaWriter.containsAction(action));
    }
  }

  protected void basicReadWriteEqual(final byte type, final ManagedObjectState state) throws Exception {
    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    final TCObjectOutputStream out = new TCObjectOutputStream(bout);
    state.writeTo(out);
    final ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
    final TCObjectInputStream in = new TCObjectInputStream(bin);
    final ManagedObjectState state2 = ManagedObjectStateFactory.getInstance().readManagedObjectStateFrom(in, type);
    Assert.assertTrue(state.equals(state2));
  }

  public interface MyProxyInf1 {
    public int getValue();

    public void setValue(int i);
  }

  public interface MyProxyInf2 {
    public String getStringValue();

    public void setStringValue(String str);
  }

  public static class MyInvocationHandler implements InvocationHandler {
    private final Map values       = new HashMap();
    private final Map stringValues = new HashMap();

    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      if (method.getName().equals("getValue")) {
        return this.values.get(proxy);
      } else if (method.getName().equals("setValue")) {
        this.values.put(proxy, args[0]);
        return null;
      } else if (method.getName().equals("setStringValue")) {
        this.stringValues.put(proxy, args[0]);
        return null;
      } else if (method.getName().equals("getStringValue")) {
        return this.stringValues.get(proxy);
      } else if (method.getName().equals("hashCode")) { return Integer.valueOf(System.identityHashCode(proxy)); }
      return null;
    }
  }
}
