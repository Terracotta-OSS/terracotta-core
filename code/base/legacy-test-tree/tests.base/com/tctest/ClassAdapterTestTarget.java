/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.TestClientObjectManager;

import java.io.ObjectStreamField;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptation target for ClassAdapterTest
 */
public class ClassAdapterTestTarget {
  private static final String              C                                    = ClassAdapterTestTarget.class
                                                                                    .getName() + ".";

  public static final String               KEY                                  = C + "key";
  public static final String               CSTR_THROW_EXCEPTION                 = C + "cstr-throw-exception";
  public static final String               CSTR_AUTOLOCK_NO_EXCEPTION           = C + "cstr-autolock-noexception";
  public static final String               CSTR_AUTOLOCK_THROW_EXCEPTION_INSIDE = C
                                                                                  + "cstr-autolock-throw-exception-inside";

  // This isn't a real serialVersionUID, but it needs to be here for tests
  @SuppressWarnings("unused")
  private static final long                serialVersionUID                     = 42L;

  // Again, this isn't really for serialization, but it needs to be here for tests
  @SuppressWarnings("unused")
  private static final ObjectStreamField[] serialPersistentFields               = { new java.io.ObjectStreamField(
                                                                                                                  "foo",
                                                                                                                  char[].class) };

  private static TestClientObjectManager   testClientObjectManager;

  List                                     myRoot                               = new ArrayList();

  public synchronized static void setTestClientObjectManager(TestClientObjectManager clientObjectManager) {
    testClientObjectManager = clientObjectManager;
  }

  public ClassAdapterTestTarget() {
    String s = System.getProperty(KEY);

    if (s != null) {
      if (CSTR_THROW_EXCEPTION.equals(s)) { throw new RuntimeException(s); }

      // This funny looking code is here to create mulitple exit paths from this constructor
      // It is also here to get some autolocking going on
      testClientObjectManager.sharedIfManaged(s);
      synchronized (s) {
        if (CSTR_AUTOLOCK_THROW_EXCEPTION_INSIDE.equals(s)) { throw new RuntimeException(s); }

        if (!CSTR_AUTOLOCK_NO_EXCEPTION.equals(s)) { throw new AssertionError(s); }

        if (hashCode() != hashCode()) { return; }
      }
    }
  }

  public void doStuff() {
    myRoot.add(this);
  }

  public synchronized void synchronizedInstanceMethodWithWideArgs(double d, long l) {
    System.out.println("You called synchronizedInstanceMethodWithWideArgs(double, long)!");
  }

  /**
   * This is a method that should be called to test a lock.
   */
  public void instanceMethod() {
    System.out.println("You called instanceMethod()!");
  }

  public void instanceMethodThrowsException() throws LockTestThrowsExceptionException {
    System.out.println("You called lockTestThrowsException()!  About to throw an exception...");
    throw new LockTestThrowsExceptionException();
  }

  public void instanceMethodWithArguments(int i) {
    System.out.println("You called instanceMethodWithArguments(" + i + ")");
  }

  public void instanceMethodWithArguments(int i, String s) {
    System.out.println("You called instanceMethodWithArguments(" + i + ", " + s + ")");
  }

  public void instanceMethodWithArgumentsThrowsException(int i, String s) throws LockTestThrowsExceptionException {
    System.out.println("You called instanceMethodWithArgumentsThrowsException");
    throwException();
  }

  public synchronized void synchronizedInstanceMethod() {
    System.out.println("You called synchronizedInstanceMethod()");
  }

  public synchronized void synchronizedInstanceMethodThrowsException() throws LockTestThrowsExceptionException {
    System.out.println("You called synchronizedInstanceMethodThrowsException()!  About to throw an exception...");
    throw new LockTestThrowsExceptionException();
  }

  public synchronized void synchronizedInstanceMethodWithArguments(int i, String s) {
    System.out.println("You called synchronizedInstanceMethodWithArguments");
  }

  public synchronized void synchronizedInstanceMethodWithArgumentsThrowsException(int i, String s)
      throws LockTestThrowsExceptionException {
    System.out.println("You called synchronizedInstanceMethodWithArgumentsThrowsException(int, String)");
    throwException();
  }

  public void internalSynchronizedInstanceMethod() {
    synchronized (this) {
      System.out.println("You called synchronizedInstanceMethod()!");
    }
  }

  public void internalSynchronizedInstanceMethodThrowsException() throws LockTestThrowsExceptionException {

    synchronized (this) {
      System.out.println("You called internalSynchronizedInstanceMethodThrowsException");
      throwException();
    }
  }

  public void internalSynchronizedInstanceMethodWithArguments(int i, String s) {
    synchronized (this) {
      System.out.println("You called internalSynchronizedInstanceMethodWithArguments(int, String)");
    }
  }

  public void internalSynchronizedInstanceMethodWithArgumentsThrowsException(int i, String s)
      throws LockTestThrowsExceptionException {
    synchronized (this) {
      System.out.println("You called internalSynchronizedInstanceMethodWithArgumentsThrowsException(int, String)");
      throwException();
    }
  }

  public static void staticMethod() {
    System.out.println("You called staticMethod()!");
  }

  public static void staticMethodThrowsException() throws Exception {
    throwException();
  }

  public static void staticMethodWithArguments(int i, String s) {
    System.out.println("You called staticMethodWithArguments(" + i + ", " + s + ")");
  }

  public static void staticMethodWithArgumentsThrowsException(int i, String s) throws Exception {
    System.out.println("You called staticMethodWithArgumentsThrowsException(" + i + ", " + s + ")");
    if (System.currentTimeMillis() > 0) { throw new LockTestThrowsExceptionException(); }
  }

  public static synchronized void synchronizedStaticMethod() {
    System.out.println("You called synchronizedStaticMethod()");
  }

  public static synchronized void synchronizedStaticMethodThrowsException() throws LockTestThrowsExceptionException {
    System.out.println("You called synchronizedStaticMethodThrowsException()");
    throwException();
  }

  public static synchronized void synchronizedStaticMethodWithArguments(int i, String s) {
    System.out.println("You called synchronizedStaticMethodWithArguments");
  }

  public static synchronized void synchronizedStaticMethodWithArgumentsThrowsException(int i, String s)
      throws LockTestThrowsExceptionException {
    System.out.println("You called synchronizedStaticMethodWithArgumentsThrowsException(int, String)");
    throwException();
  }

  public static void internalSynchronizedStaticMethod() {
    Object o = new Object();
    System.out.println("internalSynchronizedStaticMethod(): About to synchronized on " + o + "...");
    testClientObjectManager.sharedIfManaged(o);
    synchronized (o) {
      System.out.println("You called internalSynchronizedStaticMethod()");
    }
  }

  public static void internalSynchronizedStaticMethodThrowsException() throws LockTestThrowsExceptionException {
    Object o = new Object();
    testClientObjectManager.sharedIfManaged(o);
    synchronized (o) {
      System.out.println("You called internalSynchronizedStaticMethodThrowsException()");
      throwException();
    }
  }

  public static void internalSynchronizedStaticMethodWithArguments(int i, String s) {
    Object o = new Object();
    testClientObjectManager.sharedIfManaged(o);
    synchronized (o) {
      System.out.println("You called internalSynchronizedStaticMethodWithArguments(int, String)");
    }
  }

  public static void internalSynchronizedStaticMethodWithArgumentsThrowsException(int i, String s)
      throws LockTestThrowsExceptionException {
    Object o = new Object();
    testClientObjectManager.sharedIfManaged(o);
    synchronized (o) {
      System.out.println("You called internalSynchronizedStaticMethodWithArgumentsThrowsException(int, String)");
      throwException();
    }
  }

  public String instanceMethodReturnsAValue() {
    String rv = "some return value";
    System.out.println("You called instanceMethodReturnsAValue().  Returning: " + rv);
    return rv;
  }

  public String instanceMethodReturnsAValueThrowsException() throws LockTestThrowsExceptionException {
    String rv = "You called instanceMethodReturnsAValueThrowsException()";
    System.out.println(rv);
    throwException();
    return rv;
  }

  public String instanceMethodWithArgumentsReturnsAValue(int i, String s) {
    String rv = "You called instanceMethodWithArgumentsReturnsAValue(int, String)";
    System.out.println(rv);
    return rv;
  }

  public String instanceMethodWithArgumentsReturnsAValueThrowsException(int i, String s)
      throws LockTestThrowsExceptionException {
    String rv = "You called instanceMethodWithArgumentsReturnsAValueThrowsException(int, String)";
    System.out.println(rv);
    throwException();
    return rv;
  }

  public synchronized String synchronizedInstanceMethodReturnsAValue() {
    String rv = "some return value";
    System.out.println("You called synchronizedInstanceMethodReturnsAValue().  Returning " + rv);
    return rv;
  }

  public synchronized String synchronizedInstanceMethodReturnsAValueThrowsException()
      throws LockTestThrowsExceptionException {
    String rv = "You called synchronizedInstanceMethodReturnsAValueThrowsException()";
    System.out.println(rv);
    throwException();
    return rv;
  }

  public synchronized String synchronizedInstanceMethodWithArgumentsReturnsAValue(int i, String s) {
    String rv = "You called synchronizedInstanceMethodWithArgumentsReturnsAValue(int, String)";
    System.out.println(rv);
    return rv;
  }

  public synchronized String synchronizedInstanceMethodWithArgumentsReturnsAValueThrowsException(int i, String s)
      throws LockTestThrowsExceptionException {
    String rv = "You called synchronizedInstanceMethodWithArgumentsReturnsAValueThrowsException(int, String)";
    System.out.println("rv");
    throwException();
    return rv;
  }

  public String internalSynchronizedInstanceMethodReturnsAValue() {
    String rv = "You called internalSynchronizedInstanceMethodReturnsAValue()";
    synchronized (this) {
      System.out.println(rv);
    }
    return rv;
  }

  public String internalSynchronizedInstanceMethodReturnsAValueThrowsException()
      throws LockTestThrowsExceptionException {
    String rv = "You called internalSynchronizedInstanceMethodReturnsAValueThrowsException()";
    synchronized (this) {
      System.out.println(rv);
      throwException();
    }
    return rv;
  }

  public String internalSynchronizedInstanceMethodWithArgumentsReturnsAValue(int i, String s) {
    String rv = "You called internalSynchronizedInstanceMethodWithArgumentsReturnsAValue(int, String)";
    synchronized (this) {
      System.out.println(rv);
    }
    return rv;
  }

  public String internalSynchronizedInstanceMethodWithArgumentsReturnsAValueThrowsException(int i, String s)
      throws LockTestThrowsExceptionException {
    String rv = "You called internalSynchronizedInstanceMethodWithArgumentsReturnsAValueThrowsException";
    synchronized (this) {
      System.out.println(rv);
      throwException();
    }
    return rv;
  }

  public static String staticMethodReturnsAValue() {
    String rv = "You called staticMethodReturnsAValue";
    System.out.println(rv);
    return rv;
  }

  public static String staticMethodReturnsAValueThrowsException() throws Exception {
    String rv = "You called staticMethodReturnsAValueThrowsException()";
    System.out.println(rv);
    throwException();
    return rv;
  }

  public static String staticMethodWithArgumentsReturnsAValue(int i, String s) {
    String rv = "You called staticMethodWithArgumentsReturnsAValue(int, String)";
    System.out.println(rv);
    return rv;
  }

  public static String staticMethodWithArgumentsReturnsAValueThrowsException(int i, String s) throws Exception {
    String rv = "You called staticMethodWithArgumentsReturnsAValueThrowsException(int, String)";
    throwException();
    return rv;
  }

  public static synchronized String synchronizedStaticMethodReturnsAValue() {
    String rv = "You called synchronizedStaticMethodReturnsAValue()";
    System.out.println(rv);
    return rv;
  }

  public static synchronized String synchronizedStaticMethodReturnsAValueThrowsException()
      throws LockTestThrowsExceptionException {
    String rv = "You called synchronizedStaticMethodReturnsAValueThrowsException()";
    System.out.println(rv);
    throwException();
    return rv;
  }

  public static synchronized String synchronizedStaticMethodWithArgumentsReturnsAValue(int i, String s) {
    String rv = "You called synchronizedStaticMethodWithArgumentsReturnsAValue(int, String)";
    System.out.println(rv);
    return rv;
  }

  public static synchronized String synchronizedStaticMethodWithArgumentsReturnsAValueThrowsException(int i, String s)
      throws LockTestThrowsExceptionException {
    String rv = "You called synchronizedStaticMethodWithArgumentsReturnsAValueThrowsException(int i, String s)";
    System.out.println(rv);
    throwException();
    return rv;
  }

  public static String internalSynchronizedStaticMethodReturnsAValue() {
    String rv = "You called internalSynchronizedStaticMethodReturnsAValue()";
    Object o = new Object();
    testClientObjectManager.sharedIfManaged(o);
    synchronized (o) {
      System.out.println(rv);
    }
    return rv;
  }

  public static String internalSynchronizedStaticMethodReturnsAValueThrowsException()
      throws LockTestThrowsExceptionException {
    String rv = "You called internalSynchronizedStaticMethodReturnsAValueThrowsException()";
    Object o = new Object();
    testClientObjectManager.sharedIfManaged(o);
    synchronized (o) {
      System.out.println(rv);
      throwException();
    }
    return rv;
  }

  public static String internalSynchronizedStaticMethodWithArgumentsReturnsAValue(int i, String s) {
    String rv = "You called internalSynchronizedStaticMethodWithArgumentsReturnsAValue(int, String)";
    Object o = new Object();
    testClientObjectManager.sharedIfManaged(o);
    synchronized (o) {
      System.out.println(rv);
    }
    return rv;
  }

  public static String internalSynchronizedStaticMethodWithArgumentsReturnsAValueThrowsException(int i, String s)
      throws LockTestThrowsExceptionException {
    String rv = "You called internalSynchronizedStaticMethodWithArgumentsReturnsAValueThrowsException(int, String)";
    Object o = new Object();
    testClientObjectManager.sharedIfManaged(o);
    synchronized (o) {
      System.out.println(rv);
      throwException();
    }
    return rv;
  }

  public int nestedInternalSynchronizedInstanceMethod() {
    Object obj1 = new Object();
    Object obj2 = new Object();
    testClientObjectManager.sharedIfManaged(obj1);
    testClientObjectManager.sharedIfManaged(obj2);

    synchronized (obj1) {
      System.out.println("Synchronized on obj1");
      synchronized (obj2) {
        System.out.println("Synchronized on obj2");
        synchronized (this) {
          System.out.println("Synchonized on this");
          // return the number of synchronized blocks.
          return 3;
        }
      }
    }
  }

  private static void throwException() throws LockTestThrowsExceptionException {
    if (System.currentTimeMillis() > 0) { throw new LockTestThrowsExceptionException(); }
  }

  public static void main(String[] args) {
    //
  }
}
