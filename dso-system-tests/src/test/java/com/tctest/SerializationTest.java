/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.IOUtils;

import com.tc.exception.TCRuntimeException;
import com.tc.lcp.LinkedJavaProcess;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.MockRemoteSearchRequestManager;
import com.tc.object.TestClientObjectManager;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.loaders.IsolationClassLoader;
import com.tc.object.locks.MockClientLockManager;
import com.tc.object.tools.BootJar;
import com.tc.object.tx.MockTransactionManager;
import com.tc.process.StreamCollector;
import com.tc.util.runtime.Vm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/*
 * TODO:: More tests needed. 1) Populated collections needs to be serialized and deserialized. 2) Serialized
 * instrumented version of collections/objects should be deserializable by uninstrumented versions and vice versa
 */
public class SerializationTest extends BaseDSOTestCase {
  private static final Set reentrantReadWriteLockInnerClassNames = new HashSet();

  private static final Set disabled                              = new HashSet();

  static {
    disabled.add("java.util.concurrent.ConcurrentHashMap");
    disabled.add("java.util.concurrent.LinkedBlockingQueue");
    disabled.add("com.tcclient.util.concurrent.locks.ConditionObject");
    disabled.add("com.tcclient.util.concurrent.locks.ConditionObject$SyncCondition");

    reentrantReadWriteLockInnerClassNames.add("java.util.concurrent.locks.ReentrantReadWriteLock$ReadLock");
    reentrantReadWriteLockInnerClassNames.add("java.util.concurrent.locks.ReentrantReadWriteLock$WriteLock");
  }

  public void testSerialization() throws Exception {
    BootJar bj = BootJar.getDefaultBootJarForReading();

    Set specs = bj.getAllPreInstrumentedClasses();
    for (Iterator iter = specs.iterator(); iter.hasNext();) {
      String className = (String) iter.next();
      checkSerialization(className);
    }
  }

  public void testCollectionInnerClasses() throws Exception {
    validateSerialization(Collections.EMPTY_LIST);
    validateSerialization(Collections.EMPTY_SET);
    validateSerialization(Collections.EMPTY_MAP);

    // this tests Collections$UnmodifiableList
    validateSerialization(Collections.unmodifiableList(new LinkedList()));
    // this tests Collections$UnmodifiableRandomAccessList
    validateSerialization(Collections.unmodifiableList(new ArrayList()));

    validateSerialization(Collections.unmodifiableCollection(new LinkedList()));
    validateSerialization(Collections.synchronizedList(new LinkedList()));
    validateSerialization(Collections.synchronizedList(new ArrayList()));
    validateSerialization(Collections.synchronizedMap(new HashMap()));
    validateSerialization(Collections.synchronizedSet(new HashSet()));
    validateSerialization(Collections.synchronizedCollection(new LinkedList()));
  }

  public void testIfTheTestIsRunningWithBootJar() throws Exception {
    assertTrue(isHashMapDSOInstrumented());
  }

  public void testSubclassOfMapSerialization() throws Exception {
    validateSubclassOfMapSerialization(MyHashMap.class.getName());

    validateSubclassOfMapSerialization(MyHashtable.class.getName());
  }

  public void testSubclassofCollectionSerialization() throws Exception {
    validateSubclassOfCollectionSerialization(MyArrayList.class.getName());
    validateSubclassOfCollectionSerialization(MyHashSet.class.getName());
  }

  private void validateSubclassOfMapSerialization(String mapclassName) throws Exception {
    ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();

    try {
      DSOClientConfigHelper config = createClientConfigHelper();
      config.addIncludePattern(SerializationTest.class.getName() + "$*");

      TestClientObjectManager testClientObjectManager = new TestClientObjectManager();
      MockTransactionManager testTransactionManager = new MockTransactionManager();
      MockClientLockManager testClientLockManager = new MockClientLockManager();
      MockRemoteSearchRequestManager testSearchRequestManager = new MockRemoteSearchRequestManager();
      IsolationClassLoader classLoader = new IsolationClassLoader(config, testClientObjectManager,
                                                                  testTransactionManager, testClientLockManager,
                                                                  testSearchRequestManager);
      classLoader.init();
      Thread.currentThread().setContextClassLoader(classLoader);

      Class clazz = classLoader.loadClass(mapclassName);
      Map o = (Map) clazz.newInstance();
      o.put("key1", "value1");
      o.put("key2", "value2");
      assertTrue(o instanceof Manageable);
      assertTrue(o instanceof TransparentAccess);
      validateSerializationForSubclass(o, classLoader);
    } finally {
      Thread.currentThread().setContextClassLoader(originalLoader);
    }
  }

  private void validateSubclassOfCollectionSerialization(String mapclassName) throws Exception {
    ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();

    try {
      DSOClientConfigHelper config = createClientConfigHelper();
      config.addIncludePattern(SerializationTest.class.getName() + "$*");

      TestClientObjectManager testClientObjectManager = new TestClientObjectManager();
      MockTransactionManager testTransactionManager = new MockTransactionManager();
      MockClientLockManager testClientLockManager = new MockClientLockManager();
      MockRemoteSearchRequestManager testSearchRequestManager = new MockRemoteSearchRequestManager();
      IsolationClassLoader classLoader = new IsolationClassLoader(config, testClientObjectManager,
                                                                  testTransactionManager, testClientLockManager,
                                                                  testSearchRequestManager);
      classLoader.init();
      Thread.currentThread().setContextClassLoader(classLoader);

      Class clazz = classLoader.loadClass(mapclassName);
      Collection o = (Collection) clazz.newInstance();
      o.add("value1");
      o.add("value2");
      assertTrue(o instanceof Manageable);
      assertTrue(o instanceof TransparentAccess);
      validateSerializationForSubclass(o, classLoader);
    } finally {
      Thread.currentThread().setContextClassLoader(originalLoader);
    }
  }

  private static boolean isHashMapDSOInstrumented() {
    Class c = HashMap.class;
    Class[] interfaces = c.getInterfaces();
    for (Class interface1 : interfaces) {
      if (interface1.getName().equals(Manageable.class.getName())) { return true; }
    }

    return false;
  }

  private boolean handleSpecificSerializationTestIfNeeded(String className) throws Exception {
    if (reentrantReadWriteLockInnerClassNames.contains(className)) {
      checkSerializationForReentrantReadWriteLockInnerClass(className);
      return true;
    }
    return false;
  }

  private void checkSerialization(String className) throws Exception {
    boolean handleSpecificSerializationTest = handleSpecificSerializationTestIfNeeded(className);
    if (handleSpecificSerializationTest) { return; }

    if (className.startsWith("com.tc.") || (className.indexOf('$') > 0)) {
      // System.err.println("Skipping class " + className);
      return;
    }

    Class klass = Class.forName(className);

    int access = klass.getModifiers();
    if (Modifier.isAbstract(access) || !Modifier.isPublic(access)) {
      // System.err.println("Skipping " + klass);
      return;
    }

    Constructor cstr;
    try {
      cstr = klass.getConstructor(new Class[] {});
    } catch (Exception e) {
      // System.err.println("No default cstr for " + klass);
      return;
    }

    Object o = cstr.newInstance(new Object[] {});

    if (!(o instanceof Serializable)) {
      System.err.println("Skipping non-serializable " + klass);
      return;
    }

    validateSerialization(o);

    if (canValidateExternal(o)) {
      validateExternalSerialization(o);
    }
  }

  private void checkSerializationForReentrantReadWriteLockInnerClass(String innerClassName) throws Exception {
    Object lock = getReentrantReadWriteLockInnerClassInstance(innerClassName);

    validateSerialization(lock);

    if (canValidateExternal(lock)) {
      validateExternalSerialization(lock);
    }
  }

  private Object getReentrantReadWriteLockInnerClassInstance(String innerClassName) throws Exception {
    Class reentrantReadWriteLockClass = Class.forName("java.util.concurrent.locks.ReentrantReadWriteLock");
    Constructor cstr = reentrantReadWriteLockClass.getConstructor(new Class[] {});
    Object reentrantReadWriteLock = cstr.newInstance(new Object[] {});

    Class syncClass = Class.forName("java.util.concurrent.locks.ReentrantReadWriteLock$Sync");

    Object sync = getReentrantReadWriteLockField(reentrantReadWriteLock, "sync");

    Class innerClass = Class.forName(innerClassName);
    cstr = innerClass.getDeclaredConstructor(new Class[] { reentrantReadWriteLockClass, syncClass });
    cstr.setAccessible(true);
    Object innerObject = cstr.newInstance(new Object[] { reentrantReadWriteLock, sync });

    return innerObject;
  }

  private boolean canValidateExternal(Object o) {
    // Serialization of IdentityHashMap is busted on 1.4.x (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4821217)
    if (o.getClass() == IdentityHashMap.class && Vm.isJDK14()) {
      System.err.println("Skipping " + o.getClass() + " due to Java bug 4821217");
      return false;
    }
    return true;
  }

  private void validateSerialization(Object o) throws Exception {
    System.out.println("TESTING " + o.getClass());
    assertTrue(o instanceof Serializable);

    deserialize(serialize(o), o.getClass().getClassLoader());
  }

  private void validateSerializationForSubclass(Object o, ClassLoader loader) throws Exception {
    System.out.println("TESTING " + o.getClass());
    assertTrue(o instanceof Serializable);

    deserialize(serialize(o), loader);
  }

  private void validateExternalSerialization(Object o) throws Exception {
    // TODO:: This needs to be fixed.
    if (disabled.contains(o.getClass().getName())) {
      System.out.println("SKIPPING External Serialization : " + o.getClass());
      return;
    }
    System.out.println("TESTING External Serialization : " + o.getClass());
    populateData(o);
    Object n = externallySerialize(o);
    verify(o, n);
  }

  private void verify(Object o, Object n) {
    Class co = o.getClass();
    Class cn = n.getClass();
    if (co.getName().equals(cn.getName())) {
      if (o instanceof IdentityHashMap) {
        // Special check
        verifyIdentifyHashMap((IdentityHashMap) o, (IdentityHashMap) n);
      } else if (!equalsMethodPresent(co)) {
        verifyStringifiedVersion(o, n);
      } else {
        assertEquals(o, n);
      }
    } else {
      System.err.println("FATAL : Error trying serialize " + o);
      System.err.println("FATAL : Recd " + n);
      throw new AssertionError(n);
    }
  }

  private boolean equalsMethodPresent(Class c) {
    Class oc = Object.class;
    Class[] params = new Class[] { oc };
    while (c != oc) {
      try {
        c.getDeclaredMethod("equals", params);
        // Got it
        break;
      } catch (SecurityException e) {
        throw new AssertionError(e);
      } catch (NoSuchMethodException e) {
        // try the super class
        c = c.getSuperclass();
      }
    }
    return c != oc;
  }

  private void verifyStringifiedVersion(Object o, Object n) {
    String so = String.valueOf(o);
    String sn = String.valueOf(n);
    if (so != null && so.startsWith(o.getClass().getName() + "@")) {
      // This is a result from Object.toString()
      assertTrue(sn.startsWith(o.getClass().getName() + "@"));
    } else {
      assertEquals(so, sn);
    }
  }

  private void verifyIdentifyHashMap(IdentityHashMap map1, IdentityHashMap map2) {
    Map m1 = new HashMap(map1);
    Map m2 = new HashMap(map2);
    assertEquals(m1, m2);
  }

  private static Object deserialize(byte[] data, ClassLoader loader) throws IOException, ClassNotFoundException {
    // We need to use a subclass of ObjectInputStream because for when deserializing MyHashMap, we
    // need to use the instrumented version of MyHashMap and that could be achieved by loading it
    // using the same classloader that serialize it, i.e., IsolationClassLoader.
    ObjectInputStream ois = new MyObjectInputStream(new ByteArrayInputStream(data), loader);
    Object rv = ois.readObject();
    verifyRun(rv);
    ois.close();
    return rv;
  }

  private static void verifyRun(Object obj) {
    String className = obj.getClass().getName();
    if (reentrantReadWriteLockInnerClassNames.contains(className)) {
      invokeLockUnlockMethod(obj);
    } else if ("java.util.concurrent.locks.ReentrantReadWriteLock".equals(className)) {
      Object readLock = getReentrantReadWriteLockField(obj, "readerLock");
      invokeLockUnlockMethod(readLock);
      Object writeLock = getReentrantReadWriteLockField(obj, "writerLock");
      invokeLockUnlockMethod(writeLock);
    } else if ("java.util.concurrent.locks.ReentrantLock".equals(className)) {
      invokeLockUnlockMethod(obj);
    }
  }

  private static Object getReentrantReadWriteLockField(Object obj, String fieldName) {
    try {
      Field f = obj.getClass().getDeclaredField(fieldName);
      f.setAccessible(true);
      return f.get(obj);
    } catch (SecurityException e) {
      throw new TCRuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    }
  }

  private static void invokeLockUnlockMethod(Object obj) {
    try {
      Method lockMethod = obj.getClass().getDeclaredMethod("lock", new Class[] {});
      Method unlockMethod = obj.getClass().getDeclaredMethod("unlock", new Class[] {});
      lockMethod.invoke(obj, new Object[] {});
      unlockMethod.invoke(obj, new Object[] {});
    } catch (SecurityException e) {
      throw new TCRuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new TCRuntimeException(e);
    }
  }

  private static byte[] serialize(Object obj) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(obj);
    oos.close();
    return baos.toByteArray();
  }

  private Object externallySerialize(Object m) throws Exception {
    File base = getTempFile(m.getClass().getName());
    File out = new File(base.getAbsolutePath() + ".out");
    File in = new File(base.getAbsolutePath() + ".in");

    FileOutputStream fos = new FileOutputStream(out);
    fos.write(serialize(m));
    fos.close();

    LinkedJavaProcess process = new LinkedJavaProcess(ExternalSerializer.class.getName(), Arrays.asList(out
        .getAbsolutePath(), in.getAbsolutePath()));
    process.start();

    process.STDIN().close();
    StreamCollector stdout = new StreamCollector(process.STDOUT());
    stdout.start();
    StreamCollector stderr = new StreamCollector(process.STDERR());
    stderr.start();

    int exitCode = process.waitFor();

    stdout.join();
    stderr.join();

    if (exitCode != 0) {
      fail("exit code was " + exitCode + "\n\nSTDOUT:\n" + stdout.toString() + "\nSTDERR:\n" + stderr.toString());
    }

    ByteArrayOutputStream recvBytes = new ByteArrayOutputStream();
    FileInputStream fis = new FileInputStream(in);
    IOUtils.copy(fis, recvBytes);
    fis.close();

    return deserialize(recvBytes.toByteArray(), this.getClass().getClassLoader());
  }

  private Object populateData(Object o) {
    if (o instanceof TreeMap) {
      populateTreeMap((TreeMap) o);
    } else if (o instanceof TreeSet) {
      populateTreeSet((TreeSet) o);
    } else if (o instanceof Map) {
      populateMap((Map) o);
    } else if (o instanceof Set) {
      populateSet((Set) o);
    } else if (o instanceof List) {
      populateList((List) o);
    }
    return o;
  }

  private void populateTreeSet(TreeSet set) {
    set.add("Saravanan Subbiah");
    set.add("Tim Eck");
    set.add("Cindy Fisher");
    set.add("Steve Harris");
  }

  private void populateTreeMap(TreeMap map) {
    map.put("Saravanan", "Subbiah");
    map.put("Tim", "Eck");
    map.put("Cindy", "Fisher");
    map.put("Steve", "Harris");
  }

  private void populateMap(Map m) {
    m.put("Hello", "Saro");
    m.put(Integer.valueOf(99), Long.valueOf(88));
    m.put(new Date(), Double.valueOf(454.4545));
  }

  private void populateSet(Set set) {
    set.add("Hello Saro");
    set.add(Integer.valueOf(343));
    set.add(Long.valueOf(33434343));
    set.add(new Date());
    set.add(Double.valueOf(34343.23));
  }

  private void populateList(List list) {
    list.add("Hey you ");
    list.add(Integer.valueOf(34343));
    list.add(Long.valueOf(33434343));
    list.add(new Date());
    list.add(Double.valueOf(34343.23));
  }

  public static class ExternalSerializer {

    public static void main(String args[]) {
      if (isHashMapDSOInstrumented()) { throw new AssertionError("HashMap is instrumented in the external verifier!"); }

      ExternalSerializer e = new ExternalSerializer();
      try {
        e.execute(args);
      } catch (Throwable t) {
        t.printStackTrace();
        System.exit(1);
      }
      System.exit(0);
    }

    private void execute(String[] args) throws Exception {
      if (args.length != 2) { throw new AssertionError("Bad args: " + Arrays.asList(args)); }

      File readFrom = new File(args[0]);
      File writeTo = new File(args[1]);

      ByteArrayOutputStream readBytes = new ByteArrayOutputStream();
      FileInputStream in = new FileInputStream(readFrom);
      IOUtils.copy(in, readBytes);
      in.close();

      Object o = deserialize(readBytes.toByteArray(), this.getClass().getClassLoader());

      FileOutputStream out = new FileOutputStream(writeTo);
      out.write(serialize(o));
      out.close();
    }
  }

  public static class MyHashMap extends HashMap {
    private Object key;
    private Object value;

    public MyHashMap() {
      super();
    }

    public Object getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }
  }

  public static class MyHashtable extends Hashtable {
    private Object key;
    private Object value;

    public MyHashtable() {
      super();
    }

    public Object getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }
  }

  public static class MyArrayList extends ArrayList {
    private Object index;

    public MyArrayList() {
      super();
    }

    public Object getIndex() {
      return index;
    }
  }

  public static class MyHashSet extends HashSet {
    private Object index;

    public MyHashSet() {
      super();
    }

    public Object getIndex() {
      return index;
    }
  }

  private static class MyObjectInputStream extends ObjectInputStream {
    private static final Set  useCustomLoaderClasses = new HashSet();
    private final ClassLoader loader;

    static {
      useCustomLoaderClasses.add(MyHashMap.class.getName());
      useCustomLoaderClasses.add(MyHashtable.class.getName());
      useCustomLoaderClasses.add(MyArrayList.class.getName());
      useCustomLoaderClasses.add(MyHashSet.class.getName());
    }

    public MyObjectInputStream(InputStream inputStream, ClassLoader loader) throws IOException {
      super(inputStream);
      this.loader = loader;
    }

    @Override
    protected Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
      if (useCustomLoaderClasses.contains(desc.getName())) {
        return loader.loadClass(desc.getName());
      } else {
        return super.resolveClass(desc);
      }
    }
  }

}
