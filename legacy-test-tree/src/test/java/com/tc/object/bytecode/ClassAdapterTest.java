/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.MockRemoteSearchRequestManager;
import com.tc.object.TestClientObjectManager;
import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.LockDefinitionImpl;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.loaders.IsolationClassLoader;
import com.tc.object.locks.DsoLiteralLockID;
import com.tc.object.locks.DsoLockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.MockClientLockManager;
import com.tc.object.locks.StringLockID;
import com.tc.object.locks.MockClientLockManager.Begin;
import com.tc.object.tx.MockTransactionManager;
import com.tctest.ClassAdapterTestTarget;
import com.tctest.ClassAdapterTestTargetBase;
import com.tctest.ClassAdapterTestTargetBaseBase;
import com.tctest.LockTestThrowsExceptionException;

import java.io.ObjectStreamField;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultListModel;

/**
 * Test to see if an adapted class has all of the adaptations we expect.
 */
public class ClassAdapterTest extends ClassAdapterTestBase {
  private static final Class[]    WITH_ARGS_PARAMS = new Class[] { Integer.TYPE, String.class };
  private static final Object[]   WITH_ARGS_ARGS   = new Object[] { new Integer(1), "test string" };

  private DSOClientConfigHelper   config;
  private LockDefinition          lockDefinition;
  private IsolationClassLoader    classLoader;
  private TestClientObjectManager testClientObjectManager;
  private MockTransactionManager  testTransactionManager;
  private MockRemoteSearchRequestManager testRemoteSearchRequestManager;
  private MockClientLockManager   testLockManager;
  private String                  targetClassName  = ClassAdapterTestTarget.class.getName();        // "com.tctest.ClassAdapterTestTarget";
  private ClassLoader             origThreadContextClassLoader;

  protected void setUp() throws Exception {
    System.getProperties().remove(ClassAdapterTestTarget.KEY);
    initializeConfig();
    this.testClientObjectManager = new TestClientObjectManager();
    this.testTransactionManager = new MockTransactionManager();
    this.testLockManager = new MockClientLockManager();
    this.testRemoteSearchRequestManager = new MockRemoteSearchRequestManager();
    
    initClassLoader();
    this.origThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(this.classLoader);
  }

  private void initializeConfig() throws Exception {
    this.config = createClientConfigHelper();
  }

  private void createAutolockLockDefinition() {
    this.config.addIncludePattern(this.targetClassName);
    this.lockDefinition = new LockDefinitionImpl(LockDefinition.TC_AUTOLOCK_NAME, ConfigLockLevel.WRITE);
    this.lockDefinition.commit();
  }

  private void initClassLoader() {
    boolean isManaged = testClientObjectManager.getIsManaged();
    testClientObjectManager.setIsManaged(false);

    try {
      this.classLoader = new IsolationClassLoader(config, testClientObjectManager, testTransactionManager, testLockManager, testRemoteSearchRequestManager);
      this.classLoader.init();
    } finally {
      testClientObjectManager.setIsManaged(isManaged);
    }
  }

  private void setTargetClientObjectManager(Class clazz) throws Exception {
    Method m = clazz
        .getDeclaredMethod("setTestClientObjectManager", new Class[] { testClientObjectManager.getClass() });
    m.invoke(null, new Object[] { testClientObjectManager });
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    this.config = null;
    this.lockDefinition = null;
    this.testClientObjectManager = null;
    this.testTransactionManager = null;

    this.classLoader = null;
    Thread.currentThread().setContextClassLoader(this.origThreadContextClassLoader);
  }

  public void testNamedLockInInstanceManagedConstructor() throws Exception {
    String tcn = DefaultListModel.class.getName();
    config.addIncludePattern(tcn);
    String methodPattern = "* " + tcn + ".*(..)";
    lockDefinition = new LockDefinitionImpl("doStuff", ConfigLockLevel.WRITE);
    lockDefinition.commit();
    config.addLock(methodPattern, lockDefinition);

    Class clazz = this.classLoader.loadClass(tcn);
    clazz.newInstance();

  }

  public void testSuperclassTransients() throws Exception {
    String supersuperclass = ClassAdapterTestTargetBaseBase.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(supersuperclass);
    spec.addTransient("myString");

    String superclass = ClassAdapterTestTargetBase.class.getName();
    spec = config.getOrCreateSpec(superclass);

    spec = config.getOrCreateSpec(targetClassName);

    // this.config.addAutolock("public void " + targetClassName + ".doStuff()");
    String methodPattern = "public void " + targetClassName + ".doStuff()";
    lockDefinition = new LockDefinitionImpl("doStuff", ConfigLockLevel.WRITE);
    lockDefinition.commit();
    config.addLock(methodPattern, lockDefinition);

    Class clazz = this.classLoader.loadClass(targetClassName);
    Object o = clazz.newInstance();
    Method m = clazz.getDeclaredMethod("doStuff", new Class[0]);
    m.invoke(o, new Object[0]);
  }

  public void testAssertions() throws Exception {
    // Make sure that a bogus lock doesn't pass the test...
    LockDefinition bogus = new LockDefinitionImpl("fakeLock", ConfigLockLevel.WRITE);
    bogus.commit();
    assertFalse(checkForLock(bogus));
    assertNoTransactions();
    assertLockCount(0);
  }

  public void testWildcardPatternWithNamedLocksAdaptsOK() throws Exception {
    createNamedLockDefinition("test");
    createLockConfigurationForMethodExpression("*", "*", "(..)");
    callNoArgCtor();
    assertLockCount(1);
  }

  public void testWildcardPatternWithAutolockAdaptsOK() throws Exception {
    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("* ", "*", "(..)");

    callNoArgCtor();
  }

  public void testSynchronizedInstanceMethodWithWideArgs() throws Exception {
    String methodName = "synchronizedInstanceMethodWithWideArgs";
    createAutolockLockDefinition();
    String modifiersPattern = "*";
    String parametersPattern = "(..)";
    createLockConfigurationForMethodExpression(modifiersPattern, methodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithArgs(methodName, new Class[] { Double.TYPE, Long.TYPE }, new Object[] { new Double(0), new Long(0) });
  }

  public void testInstanceMethodWithNamedLocks() throws Exception {
    String methodName = "instanceMethod";
    createNamedLockDefinition("testLock");
    createLockConfigurationForMethodExpression("void", methodName, "()");

    assertNoTransactions();

    invokeWithNoArgs(methodName);

    assertNoAutolocks();

    assertNamedLockConditionsPostInvocation(1);

    // Make sure that a bogus lock doesn't pass the test...
    LockDefinition bogus = new LockDefinitionImpl("fakeLock", ConfigLockLevel.WRITE);
    bogus.commit();
    assertFalse(checkForLock(bogus));

  }

  public void testInstanceMethodThrowsExceptionWithNamedLocks() throws Exception {
    String methodName = "instanceMethodThrowsException";

    createNamedLockDefinition("testLock");
    createLockConfigurationForMethodExpression("void", methodName, "()");

    assertNoTransactions();

    invokeWithNoArgsAndCheckForProperException(methodName);

    // make sure we recorded the lock we expected.
    assertNamedLockConditionsPostInvocation(1);
  }

  public void testInstanceMethodWithArgumentsWithNamedLocks() throws Exception {

    String methodName = "instanceMethodWithArguments";

    createNamedLockDefinition("testLock");
    createLockConfigurationForMethodExpression("void", methodName, "(int, java.lang.String)");

    assertNoTransactions();

    invokeWithDefaultArgs(methodName);

    // check for begin transaction.
    assertNoAutolocks();
    assertNamedLockConditionsPostInvocation(1);
  }

  public void testInstanceMethodWithArgumentsThrowsExceptionWithNamedLocks() throws Exception {
    String methodName = "instanceMethodWithArgumentsThrowsException";

    createNamedLockDefinition("testLock");
    createLockConfigurationForMethodExpression("void", methodName, "(int, java.lang.String)");

    assertNoTransactions();

    invokeWithDefaultArgsAndCheckForProperException(methodName);

    // check for begin transaction.
    assertNoAutolocks();
    assertNamedLockConditionsPostInvocation(1);
  }

  public void testSynchronizedInstanceMethodWithAutolock() throws Exception {
    String testMethodName = "synchronizedInstanceMethod";

    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("public synchronized void", testMethodName, "()");

    assertNoTransactions();

    // unmanaged objects shouldn't be autolocked.
    this.testClientObjectManager.setIsManaged(false);
    invokeWithNoArgs(testMethodName);
    assertNoTransactions();

    // managed objects SHOULD be autolocked.
    this.testClientObjectManager.setIsManaged(true);
    initClassLoader();

    invokeWithNoArgs(testMethodName);

    assertAutolockConditionsPostInvocation(1);
  }

  public void testSynchronizedInstanceMethodThrowsExceptionWithAutolock() throws Exception {
    String testMethodName = "synchronizedInstanceMethodThrowsException";

    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("public synchronized void", testMethodName, "()");

    // int modifiers = getModifiers(testMethodName, new Class[] {});

    // assertTrue(config.isLockMethod(modifiers, targetClassName,
    // testMethodName, "()V", null));
    // assertTrue(config.isAutolock(modifiers, targetClassName, testMethodName,
    // "()V", null));
    assertNoTransactions();

    // unmanaged objects shouldn't be autolocked.
    this.testClientObjectManager.setIsManaged(false);
    invokeWithNoArgsAndCheckForProperException(testMethodName);

    assertNoTransactions();

    // managed objects SHOULD be autolocked.
    this.testClientObjectManager.setIsManaged(true);
    initClassLoader();
    invokeWithNoArgsAndCheckForProperException(testMethodName);

    assertAutolockConditionsPostInvocation(1);
  }

  public void testSynchronizedInstanceMethodWithArgumentsWithAutolock() throws Exception {
    String methodName = "synchronizedInstanceMethodWithArguments";

    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("public synchronized void", methodName, "(int, java.lang.String)");

    // Class[] params = new Class[] { Integer.TYPE, String.class };

    // int modifiers = getModifiers(methodName, params);

    // assertTrue(config.isLockMethod(modifiers, targetClassName, methodName,
    // "(ILjava/lang/String;)V", null));
    // assertTrue(config.isAutolock(modifiers, targetClassName, methodName,
    // "(ILjava/lang/String;)V", null));
    assertNoTransactions();

    // unmanaged objects shouldn't be autolocked.
    this.testClientObjectManager.setIsManaged(false);
    invokeWithDefaultArgs(methodName);
    assertNoTransactions();

    // managed objects SHOULD be autolocked.
    this.testClientObjectManager.setIsManaged(true);
    initClassLoader();
    invokeWithDefaultArgs(methodName);

    assertAutolockConditionsPostInvocation(1);
  }

  /**
   * Tests that autolocks work on synchronized methods.
   */
  public void testSynchronizedInstanceMethodWithArgumentsThrowsExceptionWithAutolock() throws Exception {
    String methodName = "synchronizedInstanceMethodWithArgumentsThrowsException";

    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("public synchronized void", methodName, "(int, java.lang.String)");

    // Class[] params = new Class[] { Integer.TYPE, String.class };
    //
    // int modifiers = getModifiers(methodName, params);

    // assertTrue(config.isLockMethod(modifiers, targetClassName, methodName,
    // "(ILjava/lang/String;)V", null));
    // assertTrue(config.isAutolock(modifiers, targetClassName, methodName,
    // "(ILjava/lang/String;)V", null));
    assertNoTransactions();

    // unmanaged objects shouldn't be autolocked.
    this.testClientObjectManager.setIsManaged(false);

    invokeWithDefaultArgsAndCheckForProperException(methodName);
    assertNoTransactions();

    // managed objects SHOULD be autolocked.
    this.testClientObjectManager.setIsManaged(true);
    initClassLoader();
    invokeWithDefaultArgsAndCheckForProperException(methodName);
    assertAutolockConditionsPostInvocation(1);
  }

  /**
   * Tests that autolocks work on synchronization inside methods.
   */
  public void testInternalSynchronizedInstanceMethodWithAutolock() throws Exception {
    String testMethodName = "internalSynchronizedInstanceMethod";

    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("public void", testMethodName, "()");

    // int modifiers = getModifiers(testMethodName, new Class[] {});
    //
    // assertFalse(config.isLockMethod(modifiers, targetClassName,
    // testMethodName, "()V", null));
    // assertTrue(config.isAutolock(modifiers, targetClassName, testMethodName,
    // "()V", null));
    assertNoTransactions();

    // unmanaged objects shouldn't be autolocked.
    this.testClientObjectManager.setIsManaged(false);
    invokeWithNoArgs(testMethodName);
    assertNoTransactions();

    // managed objects SHOULD be autolocked.
    this.testClientObjectManager.setIsManaged(true);
    initClassLoader();

    invokeWithNoArgs(testMethodName);

    assertAutolockConditionsPostInvocation(1);
  }

  public void testInternalSynchronizedInstanceMethodWithNamedlockAndAutoLock() throws Exception {
    config.addIncludePattern(this.targetClassName);
    String methodName = "internalSynchronizedInstanceMethod";
    // set up locks
    String methodExpression = "void " + targetClassName + "." + methodName + "()";
    LockDefinition ldnamed = new LockDefinitionImpl("test-lock", ConfigLockLevel.WRITE);
    ldnamed.commit();
    LockDefinition ldautolock = new LockDefinitionImpl(LockDefinition.TC_AUTOLOCK_NAME, ConfigLockLevel.WRITE);
    ldautolock.commit();

    config.getOrCreateSpec(targetClassName);
    config.addLock(methodExpression, ldautolock);
    config.addLock(methodExpression, ldnamed);

    this.testClientObjectManager.setIsManaged(true);

    assertNoTransactions();

    invokeWithNoArgs(methodName);

    assertTrue(checkForLockName(ldnamed.getLockName(), ldnamed.getLockLevelAsInt()));
    assertTrue(checkForLock(ldnamed));
    assertAutolockCount(1);
    assertLockCount(2);
    assertNoAutolockLiteral();
  }

  public void testInternalSynchronizedInstanceMethodThrowsExceptionWithAutolock() throws Exception {
    String testMethodName = "internalSynchronizedInstanceMethodThrowsException";

    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("public void", testMethodName, "()");

    // int modifiers = getModifiers(testMethodName, new Class[] {});
    //
    // assertFalse(config.isLockMethod(modifiers, targetClassName,
    // testMethodName, NO_ARGS_RETURNS_VOID_DESCRIPTION,
    // null));
    // assertTrue(config.isAutolock(modifiers, targetClassName, testMethodName,
    // NO_ARGS_RETURNS_VOID_DESCRIPTION,
    // null));
    assertNoTransactions();

    // unmanaged objects shouldn't be autolocked.
    this.testClientObjectManager.setIsManaged(false);
    invokeWithNoArgsAndCheckForProperException(testMethodName);
    assertNoTransactions();

    // managed objects SHOULD be autolocked.
    this.testClientObjectManager.setIsManaged(true);
    initClassLoader();

    invokeWithNoArgsAndCheckForProperException(testMethodName);
    assertAutolockConditionsPostInvocation(1);
  }

  public void testInternalSynchronizedInstanceMethodWithArgumentsWithAutolock() throws Exception {
    String testMethodName = "internalSynchronizedInstanceMethodWithArguments";
    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("public void", testMethodName, "(int, java.lang.String)");

    // int modifiers = getModifiers(testMethodName, WITH_ARGS_PARAMS);
    // assertFalse(config.isLockMethod(modifiers, targetClassName,
    // testMethodName, WITH_ARGS_RETURNS_VOID_DESCRIPTION,
    // null));
    // assertTrue(config.isAutolock(modifiers, targetClassName, testMethodName,
    // WITH_ARGS_RETURNS_VOID_DESCRIPTION,
    // null));

    // unmanaged objects shouldn't be autolocked.
    this.testClientObjectManager.setIsManaged(false);
    invokeWithDefaultArgs(testMethodName);
    assertNoTransactions();

    // managed objects should be autolocked
    this.testClientObjectManager.setIsManaged(true);
    initClassLoader();

    invokeWithDefaultArgs(testMethodName);
    assertAutolockConditionsPostInvocation(1);
  }

  public void testInternalSynchronizedInstanceMethodWithArgumentsThrowsExceptionWithAutolock() throws Exception {
    String testMethodName = "internalSynchronizedInstanceMethodWithArgumentsThrowsException";
    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("public void", testMethodName, "(int, java.lang.String)");

    // int modifiers = getModifiers(testMethodName, WITH_ARGS_PARAMS);
    // assertFalse(config.isLockMethod(modifiers, targetClassName,
    // testMethodName, WITH_ARGS_RETURNS_VOID_DESCRIPTION,
    // null));
    // assertTrue(config.isAutolock(modifiers, targetClassName, testMethodName,
    // WITH_ARGS_RETURNS_VOID_DESCRIPTION,
    // null));

    this.testClientObjectManager.setIsManaged(false);
    invokeWithDefaultArgsAndCheckForProperException(testMethodName);
    assertNoTransactions();

    this.testClientObjectManager.setIsManaged(true);
    initClassLoader();

    invokeWithDefaultArgsAndCheckForProperException(testMethodName);
    assertAutolockConditionsPostInvocation(1);
  }

  public void testStaticMethodWithNamedLocks() throws Exception {
    String methodName = "staticMethod";

    createNamedLockDefinition("testLock");
    createLockConfigurationForMethodExpression("void", methodName, "()");

    assertNoTransactions();

    invokeWithNoArgs(methodName);
    assertAutolockCount(0);
    assertNamedLockConditionsPostInvocation(1);
  }

  public void testStaticMethodThrowsExceptionWithNamedLocks() throws Exception {
    String methodName = "staticMethodThrowsException";

    createNamedLockDefinition("testLock");
    createLockConfigurationForMethodExpression("void", methodName, "()");

    assertNoTransactions();
    invokeWithNoArgsAndCheckForProperException(methodName);

    // make sure we recorded the lock we expected.
    assertNamedLockConditionsPostInvocation(1);
  }

  public void testStaticMethodWithArgumentsWithNamedLocks() throws Exception {
    String methodName = "staticMethodWithArguments";

    createNamedLockDefinition("testLock");
    createLockConfigurationForMethodExpression("void", methodName, "(int, java.lang.String)");

    assertNoTransactions();
    invokeWithDefaultArgs(methodName);
    assertNamedLockConditionsPostInvocation(1);
  }

  public void testStaticMethodWithArgumentsThrowsExceptionWithNamedLocks() throws Exception {
    String methodName = "staticMethodWithArgumentsThrowsException";

    createNamedLockDefinition("testLock");
    createLockConfigurationForMethodExpression("void", methodName, "(int, java.lang.String)");

    assertNoTransactions();
    invokeWithDefaultArgsAndCheckForProperException(methodName);
    assertNamedLockConditionsPostInvocation(1);
  }

  public void testSynchronizedStaticMethodWithAutolock() throws Exception {
    String testMethodName = "synchronizedStaticMethod";
    createAutolockLockDefinition();
    String modifiersPattern = "*";
    String parametersPattern = "(..)";
    createLockConfigurationForMethodExpression(modifiersPattern, testMethodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithNoArgs(testMethodName);

    assertNoTransactions();
    assertNoAutolocks();

    // make sure that the pattern used to test autolocks actually picks out a
    // real method.
    checkWithNamedLockNoArgs(testMethodName, modifiersPattern, parametersPattern);
  }

  public void testSynchronizedStaticMethodThrowsExceptionWithAutolock() throws Exception {
    String testMethodName = "synchronizedStaticMethodThrowsException";
    createAutolockLockDefinition();
    String modifiersPattern = "*";
    String parametersPattern = "(..)";
    createLockConfigurationForMethodExpression(modifiersPattern, testMethodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithNoArgsAndCheckForProperException(testMethodName);

    assertNoTransactions();
    assertNoAutolocks();

    // make sure that the pattern used to test autolocks actually picks out a
    // real method.
    checkWithNamedLockNoArgsThrowsException(testMethodName, modifiersPattern, parametersPattern);
  }

  public void testSynchronizedStaticMethodWithArgumentsWithAutolock() throws Exception {
    String testMethodName = "synchronizedStaticMethodWithArguments";
    createAutolockLockDefinition();
    String modifiersPattern = "*";
    String parametersPattern = "(..)";
    createLockConfigurationForMethodExpression(modifiersPattern, testMethodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithDefaultArgs(testMethodName);

    assertNoTransactions();
    assertNoAutolocks();

    // make sure that the pattern used to test autolocks actually picks out a
    // real method.
    checkWithNamedLockDefaultArgs(testMethodName, modifiersPattern, parametersPattern);
  }

  public void testSynchronizedStaticMethodWithArgumentsThrowsExceptionWithAutolock() throws Exception {
    String testMethodName = "synchronizedStaticMethodWithArgumentsThrowsException";
    createAutolockLockDefinition();
    String modifiersPattern = "*";
    String parametersPattern = "(..)";
    createLockConfigurationForMethodExpression(modifiersPattern, testMethodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithDefaultArgsAndCheckForProperException(testMethodName);

    assertNoTransactions();
    assertNoAutolocks();

    checkWithnamedLockDefaultArgsThrowsException(testMethodName, modifiersPattern, parametersPattern);
  }

  public void testInternalSynchronizedStaticMethodWithAutolock() throws Exception {
    String testMethodName = "internalSynchronizedStaticMethod";
    createAutolockLockDefinition();
    String modifiersPattern = "*";
    String parametersPattern = "(..)";
    createLockConfigurationForMethodExpression(modifiersPattern, testMethodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithNoArgs(testMethodName);

    assertAutolockConditionsPostInvocation(1);
  }

  public void testInternalSynchronizedStaticMethodThrowsExceptionWithAutolock() throws Exception {
    String testMethodName = "internalSynchronizedStaticMethodThrowsException";
    createAutolockLockDefinition();
    String modifiersPattern = "*";
    String parametersPattern = "(..)";
    createLockConfigurationForMethodExpression(modifiersPattern, testMethodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithNoArgsAndCheckForProperException(testMethodName);

    assertAutolockConditionsPostInvocation(1);
  }

  public void testInternalSynchronizedStaticMethodWithArgumentsWithAutolock() throws Exception {
    String testMethodName = "internalSynchronizedStaticMethodWithArguments";
    createAutolockLockDefinition();
    String modifiersPattern = "*";
    String parametersPattern = "(..)";
    createLockConfigurationForMethodExpression(modifiersPattern, testMethodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithDefaultArgs(testMethodName);

    assertAutolockConditionsPostInvocation(1);
  }

  public void testInternalSynchronizedStaticMethodWithArgumentsThrowsExceptionWithAutolock() throws Exception {
    String testMethodName = "internalSynchronizedStaticMethodWithArgumentsThrowsException";
    createAutolockLockDefinition();
    String modifiersPattern = "*";
    String parametersPattern = "(..)";
    createLockConfigurationForMethodExpression(modifiersPattern, testMethodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithDefaultArgsAndCheckForProperException(testMethodName);

    assertAutolockConditionsPostInvocation(1);
  }

  public void testInstanceMethodReturnsAValueWithNamedLocks() throws Exception {

    String methodName = "instanceMethodReturnsAValue";

    createNamedLockDefinition("testLock");
    createLockConfigurationForMethodExpression("*", methodName, "()");

    assertNoTransactions();

    invokeWithNoArgs(methodName);

    assertNamedLockConditionsPostInvocation(1);
  }

  public void testInstanceMethodReturnsAValueThrowsExceptionWithNamedLocks() throws Exception {
    String methodName = "instanceMethodReturnsAValueThrowsException";

    createNamedLockDefinition("testLock");
    createLockConfigurationForMethodExpression("*", methodName, "()");

    assertNoTransactions();

    invokeWithNoArgsAndCheckForProperException(methodName);

    assertNamedLockConditionsPostInvocation(1);
  }

  public void testInstanceMethodWithArgumentsReturnsAValue() throws Exception {
    String methodName = "instanceMethodWithArgumentsReturnsAValue";

    createNamedLockDefinition("testLock");
    createLockConfigurationForMethodExpression("*", methodName, "(..)");

    assertNoTransactions();

    invokeWithDefaultArgs(methodName);

    assertNamedLockConditionsPostInvocation(1);
  }

  public void testInstanceMethodWithArgumentsReturnsAValueThrowsException() throws Exception {
    String methodName = "instanceMethodWithArgumentsReturnsAValueThrowsException";

    createNamedLockDefinition("testLock");
    createLockConfigurationForMethodExpression("*", methodName, "(..)");

    assertNoTransactions();

    invokeWithDefaultArgsAndCheckForProperException(methodName);

    assertNamedLockConditionsPostInvocation(1);
  }

  public void testSynchronizedInstanceMethodReturnsAValueWithAutolock() throws Exception {
    String methodName = "synchronizedInstanceMethodReturnsAValue";

    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("String", methodName, "()");

    this.testClientObjectManager.setIsManaged(false);
    invokeWithNoArgs(methodName);

    assertNoTransactions();

    this.testClientObjectManager.setIsManaged(true);
    initClassLoader();

    invokeWithNoArgs(methodName);
    assertAutolockConditionsPostInvocation(1);
  }

  public void testSynchronizedInstanceMethodReturnsAValueThrowsExceptionWithAutolock() throws Exception {
    String methodName = "synchronizedInstanceMethodReturnsAValueThrowsException";

    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("String", methodName, "()");

    this.testClientObjectManager.setIsManaged(false);
    invokeWithNoArgsAndCheckForProperException(methodName);

    assertNoTransactions();

    this.testClientObjectManager.setIsManaged(true);
    initClassLoader();

    invokeWithNoArgsAndCheckForProperException(methodName);
    assertAutolockConditionsPostInvocation(1);
  }

  public void testSynchronizedInstanceMethodWithArgumentsReturnsAValueWithAutolock() throws Exception {
    String methodName = "synchronizedInstanceMethodWithArgumentsReturnsAValue";

    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("String", methodName, "(..)");

    this.testClientObjectManager.setIsManaged(false);
    invokeWithDefaultArgs(methodName);

    assertNoTransactions();

    this.testClientObjectManager.setIsManaged(true);
    initClassLoader();

    invokeWithDefaultArgs(methodName);
    assertAutolockConditionsPostInvocation(1);
  }

  public void testSynchronizedInstanceMethodWithArgumentsReturnsAValueThrowsExceptionWithAutolock() throws Exception {

    String methodName = "synchronizedInstanceMethodWithArgumentsReturnsAValueThrowsException";

    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("String", methodName, "(..)");

    this.testClientObjectManager.setIsManaged(false);
    invokeWithDefaultArgsAndCheckForProperException(methodName);

    assertNoTransactions();

    this.testClientObjectManager.setIsManaged(true);
    initClassLoader();

    invokeWithDefaultArgsAndCheckForProperException(methodName);

    assertAutolockConditionsPostInvocation(1);
  }

  public void testInternalSynchronizedInstanceMethodReturnsAValueWithAutolock() throws Exception {

    String methodName = "internalSynchronizedInstanceMethodReturnsAValue";

    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("String", methodName, "()");

    this.testClientObjectManager.setIsManaged(false);
    invokeWithNoArgs(methodName);

    assertNoTransactions();

    this.testClientObjectManager.setIsManaged(true);
    initClassLoader();

    invokeWithNoArgs(methodName);

    assertAutolockConditionsPostInvocation(1);
  }

  public void testInternalSynchronizedInstanceMethodReturnsAValueThrowsExceptionWithAutolock() throws Exception {
    String methodName = "internalSynchronizedInstanceMethodReturnsAValueThrowsException";

    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("String", methodName, "()");

    this.testClientObjectManager.setIsManaged(false);
    invokeWithNoArgsAndCheckForProperException(methodName);

    assertNoTransactions();

    this.testClientObjectManager.setIsManaged(true);
    initClassLoader();

    invokeWithNoArgsAndCheckForProperException(methodName);

    assertAutolockConditionsPostInvocation(1);
  }

  public void testInternalSynchronizedInstanceMethodWithArgumentsReturnsAValueWithAutolock() throws Exception {
    String methodName = "internalSynchronizedInstanceMethodWithArgumentsReturnsAValue";

    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("String", methodName, "(..)");

    this.testClientObjectManager.setIsManaged(false);
    invokeWithDefaultArgs(methodName);

    assertNoTransactions();

    this.testClientObjectManager.setIsManaged(true);
    initClassLoader();

    invokeWithDefaultArgs(methodName);

    assertAutolockConditionsPostInvocation(1);
  }

  public void testInternalSynchronizedInstanceMethodWithArgumentsReturnsAValueThrowsExceptionWithAutolock()
      throws Exception {
    String methodName = "internalSynchronizedInstanceMethodWithArgumentsReturnsAValueThrowsException";

    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("String", methodName, "(..)");

    this.testClientObjectManager.setIsManaged(false);
    invokeWithDefaultArgsAndCheckForProperException(methodName);

    assertNoTransactions();

    this.testClientObjectManager.setIsManaged(true);
    initClassLoader();

    invokeWithDefaultArgsAndCheckForProperException(methodName);

    assertAutolockConditionsPostInvocation(1);
  }

  public void testStaticMethodReturnsAValueWithNamedLocks() throws Exception {
    String methodName = "staticMethodReturnsAValue";
    createNamedLockDefinition("testLock");
    createLockConfigurationForMethodExpression("*", methodName, "()");

    assertNoTransactions();

    invokeWithNoArgs(methodName);

    assertNamedLockConditionsPostInvocation(1);
  }

  public void testStaticMethodReturnsAValueThrowsExceptionWithNamedLocks() throws Exception {
    String methodName = "staticMethodReturnsAValueThrowsException";

    createNamedLockDefinition("testLock");
    createLockConfigurationForMethodExpression("*", methodName, "()");

    assertNoTransactions();

    invokeWithNoArgsAndCheckForProperException(methodName);

    assertNamedLockConditionsPostInvocation(1);
  }

  public void testStaticMethodWithArgumentsReturnsAValueWithNamedLocks() throws Exception {
    String methodName = "staticMethodWithArgumentsReturnsAValue";
    createNamedLockDefinition("testLock");
    createLockConfigurationForMethodExpression("*", methodName, "(..)");

    assertNoTransactions();

    invokeWithDefaultArgs(methodName);

    assertNamedLockConditionsPostInvocation(1);
  }

  public void testStaticMethodWithArgumentsReturnsAValueThrowsExceptionWithNamedLocks() throws Exception {

    String methodName = "staticMethodWithArgumentsReturnsAValueThrowsException";
    createNamedLockDefinition("testLock");
    createLockConfigurationForMethodExpression("*", methodName, "(..)");

    assertNoTransactions();

    invokeWithDefaultArgsAndCheckForProperException(methodName);

    assertNamedLockConditionsPostInvocation(1);
  }

  /**
   * Autolocks should not apply to synchronized static methods because they are synchronized on the Class object which
   * can't be distributed... we don't think.
   */
  public void testSynchronizedStaticMethodReturnsAValueWithAutolocks() throws Exception {
    String testMethodName = "synchronizedStaticMethodReturnsAValue";
    createAutolockLockDefinition();
    String modifiersPattern = "*";
    String parametersPattern = "(..)";
    createLockConfigurationForMethodExpression(modifiersPattern, testMethodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithNoArgs(testMethodName);

    assertNoTransactions();
    assertNoAutolocks();

    // make sure that the pattern used to test autolocks actually picks out a
    // real method.
    checkWithNamedLockNoArgs(testMethodName, modifiersPattern, parametersPattern);
  }

  public void testSynchronizedStaticMethodReturnsAValueThrowsExceptionWithAutolocks() throws Exception {
    String testMethodName = "synchronizedStaticMethodReturnsAValueThrowsException";
    createAutolockLockDefinition();
    String modifiersPattern = "*";
    String parametersPattern = "(..)";
    createLockConfigurationForMethodExpression(modifiersPattern, testMethodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithNoArgsAndCheckForProperException(testMethodName);

    assertNoTransactions();
    assertNoAutolocks();

    // make sure that the pattern used to test autolocks actually picks out a
    // real method.
    checkWithNamedLockNoArgsThrowsException(testMethodName, modifiersPattern, parametersPattern);
  }

  public void testSynchronizedStaticMethodWithArgumentsReturnsAValueWithAutolocks() throws Exception {
    String testMethodName = "synchronizedStaticMethodWithArgumentsReturnsAValue";
    createAutolockLockDefinition();
    String modifiersPattern = "*";
    String parametersPattern = "(..)";
    createLockConfigurationForMethodExpression(modifiersPattern, testMethodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithDefaultArgs(testMethodName);

    assertNoTransactions();
    assertNoAutolocks();

    // make sure that the pattern used to test autolocks actually picks out a
    // real method.
    checkWithNamedLockDefaultArgs(testMethodName, modifiersPattern, parametersPattern);
  }

  public void testSynchronizedStaticMethodWithArgumentsReturnsAValueThrowsExceptionWithAutolocks() throws Exception {
    String testMethodName = "synchronizedStaticMethodWithArgumentsReturnsAValueThrowsException";
    createAutolockLockDefinition();
    String modifiersPattern = "*";
    String parametersPattern = "(..)";
    createLockConfigurationForMethodExpression(modifiersPattern, testMethodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithDefaultArgsAndCheckForProperException(testMethodName);

    assertNoTransactions();
    assertNoAutolocks();

    checkWithnamedLockDefaultArgsThrowsException(testMethodName, modifiersPattern, parametersPattern);
  }

  /**
   * Autolocks SHOULD apply to internally synchronized static methods.
   */
  public void testInternalSynchronizedStaticMethodReturnsAValueWithAutolocks() throws Exception {

    String testMethodName = "internalSynchronizedStaticMethodReturnsAValue";
    createAutolockLockDefinition();
    String modifiersPattern = "*";
    String parametersPattern = "(..)";
    createLockConfigurationForMethodExpression(modifiersPattern, testMethodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithNoArgs(testMethodName);

    assertAutolockConditionsPostInvocation(1);
  }

  public void testInternalSynchronizedStaticMethodReturnsAValueThrowsExceptionWithAutolocks() throws Exception {

    String testMethodName = "internalSynchronizedStaticMethodReturnsAValueThrowsException";
    createAutolockLockDefinition();
    String modifiersPattern = "*";
    String parametersPattern = "(..)";
    createLockConfigurationForMethodExpression(modifiersPattern, testMethodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithNoArgsAndCheckForProperException(testMethodName);

    assertAutolockConditionsPostInvocation(1);
  }

  public void testInternalSynchronizedStaticMethodWithArgumentsReturnsAValueWithAutolocks() throws Exception {

    String testMethodName = "internalSynchronizedStaticMethodWithArgumentsReturnsAValue";
    createAutolockLockDefinition();
    String modifiersPattern = "*";
    String parametersPattern = "(..)";
    createLockConfigurationForMethodExpression(modifiersPattern, testMethodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithDefaultArgs(testMethodName);

    assertAutolockConditionsPostInvocation(1);
  }

  public void testInternalSynchronizedStaticMethodWithArgumentsReturnsAValueThrowsExceptionWithAutolocks()
      throws Exception {

    String testMethodName = "internalSynchronizedStaticMethodWithArgumentsReturnsAValueThrowsException";
    createAutolockLockDefinition();
    String modifiersPattern = "*";
    String parametersPattern = "(..)";
    createLockConfigurationForMethodExpression(modifiersPattern, testMethodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithDefaultArgsAndCheckForProperException(testMethodName);

    assertAutolockConditionsPostInvocation(1);
  }

  public void testInstanceMethodWithAutolockAndNoSynchronizersDoesNothing() throws Exception {
    String methodName = "instanceMethod";

    createAutolockLockDefinition();
    String modifiersPattern = "void";
    String parametersPattern = "()";
    createLockConfigurationForMethodExpression(modifiersPattern, methodName, parametersPattern);

    this.testClientObjectManager.setIsManaged(true);
    invokeWithNoArgs(methodName);
    assertNoTransactions();

    // add a regular-old named lock to make sure that the method expression
    // actually picks out a method in order
    // to validate that the test above actually tested something...
    checkWithNamedLockNoArgs(methodName, modifiersPattern, parametersPattern);
  }

  public void testWildcardAutolock() throws Exception {

    String testMethodName = "internalSynchronizedInstanceMethod";

    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("*", "*", "(..)");
    //
    // int modifiers = getModifiers(testMethodName, new Class[] {});
    //
    // assertFalse(config.isLockMethod(modifiers, targetClassName,
    // testMethodName, "()V", null));
    // assertTrue(config.isAutolock(modifiers, targetClassName, testMethodName,
    // "()V", null));
    assertNoTransactions();

    // unmanaged objects shouldn't be autolocked.
    this.testClientObjectManager.setIsManaged(false);
    invokeWithNoArgs(testMethodName);
    assertNoTransactions();

    // managed objects SHOULD be autolocked.
    this.testClientObjectManager.setIsManaged(true);
    initClassLoader();

    invokeWithNoArgs(testMethodName);

    assertAutolockConditionsPostInvocation(1);
  }

  public void testReadLock() throws Exception {
    config.addIncludePattern(this.targetClassName);
    String methodName = "instanceMethod";

    this.lockDefinition = new LockDefinitionImpl("testReadLock", ConfigLockLevel.READ);
    this.lockDefinition.commit();

    createLockConfigurationForMethodExpression("void", methodName, "()");

    assertNoTransactions();

    invokeWithNoArgs(methodName);
    assertNamedLockConditionsPostInvocation(1);
  }

  public void testWithoutLocks() throws Exception {
    String methodName = "internalSynchronizedInstanceMethod";

    invokeWithNoArgs(methodName);

    assertLockCount(0);
  }

  public void testAutolockCtorNoException() throws Exception {
    String methodExpression = "* " + targetClassName + ".*(..)";
    LockDefinition ld = new LockDefinitionImpl(LockDefinition.TC_AUTOLOCK_NAME, ConfigLockLevel.WRITE);
    ld.commit();

    config.getOrCreateSpec(targetClassName);
    config.addLock(methodExpression, ld);

    this.testClientObjectManager.setIsManaged(true);

    assertNoTransactions();

    System.setProperty(ClassAdapterTestTarget.KEY, ClassAdapterTestTarget.CSTR_AUTOLOCK_NO_EXCEPTION);

    callNoArgCtor();
    assertLockCount(1);
    assertAutolockCount(1);
  }

  public void testNamedlockCtorNoException() throws Exception {
    String methodExpression = "* " + targetClassName + ".*(..)";
    LockDefinition ld = new LockDefinitionImpl("test-lock", ConfigLockLevel.WRITE);
    ld.commit();

    config.getOrCreateSpec(targetClassName);
    config.addLock(methodExpression, ld);

    assertNoTransactions();

    callNoArgCtor();
    assertLockCount(1);
    assertAutolockCount(0);
  }

  public void testNamedlockCtorThrowsException() throws Exception {
    String methodExpression = "* " + targetClassName + ".*(..)";
    LockDefinition ld = new LockDefinitionImpl("test-lock", ConfigLockLevel.WRITE);
    ld.commit();

    config.getOrCreateSpec(targetClassName);
    config.addLock(methodExpression, ld);

    assertNoTransactions();

    System.setProperty(ClassAdapterTestTarget.KEY, ClassAdapterTestTarget.CSTR_THROW_EXCEPTION);

    try {
      callNoArgCtor();
      throw new AssertionError();
    } catch (RuntimeException re) {
      if ((re.getClass() != RuntimeException.class)
          || !re.getMessage().equals(ClassAdapterTestTarget.CSTR_THROW_EXCEPTION)) { throw re; }
    }

    assertLockCount(1);
    assertAutolockCount(0);
  }

  private void testAutolockCtorException(boolean inside) throws Exception {
    String methodExpression = "* " + targetClassName + ".*(..)";
    LockDefinition ld = new LockDefinitionImpl(LockDefinition.TC_AUTOLOCK_NAME, ConfigLockLevel.WRITE);
    ld.commit();

    config.getOrCreateSpec(targetClassName);
    config.addLock(methodExpression, ld);

    this.testClientObjectManager.setIsManaged(true);

    assertNoTransactions();

    String cmd = inside ? ClassAdapterTestTarget.CSTR_AUTOLOCK_THROW_EXCEPTION_INSIDE
        : ClassAdapterTestTarget.CSTR_THROW_EXCEPTION;

    System.setProperty(ClassAdapterTestTarget.KEY, cmd);

    try {
      callNoArgCtor();
      throw new AssertionError();
    } catch (RuntimeException re) {
      if ((re.getClass() != RuntimeException.class) || !re.getMessage().equals(cmd)) { throw re; }
    }

    int numTxn = inside ? 1 : 0;

    assertLockCount(numTxn);
    assertAutolockCount(numTxn);
  }

  public void testAutolockCtorExceptionOutsideSynch() throws Exception {
    testAutolockCtorException(false);
  }

  public void testAutolockCtorExceptionInsideSynch() throws Exception {
    testAutolockCtorException(true);
  }

  public void testNestedAutolocks() throws Exception {
    String methodName = "nestedInternalSynchronizedInstanceMethod";

    createAutolockLockDefinition();
    createLockConfigurationForMethodExpression("*", "*", "(..)");

    this.testClientObjectManager.setIsManaged(true);
    assertNoTransactions();

    Object result = invokeWithNoArgs(methodName);
    int expectedTransactionCount = ((Integer) result).intValue();
    assertAutolockConditionsPostInvocation(expectedTransactionCount);
  }

  public void testMultipleNamedLocks() throws Exception {
    config.addIncludePattern(this.targetClassName);
    String methodName = "instanceMethod";
    String methodExpression = "* " + targetClassName + "." + methodName + "(..)";
    LockDefinition ld1 = new LockDefinitionImpl("lock1", ConfigLockLevel.WRITE);
    LockDefinition ld2 = new LockDefinitionImpl("lock2", ConfigLockLevel.WRITE);
    ld1.commit();
    ld2.commit();
    LockDefinition[] declaredLockDefs = new LockDefinition[] { ld1, ld2 };

    config.getOrCreateSpec(targetClassName);
    config.addLock(methodExpression, ld1);
    config.addLock(methodExpression, ld2);
    this.testClientObjectManager.setIsManaged(true);

    assertNoTransactions();

    invokeWithNoArgs(methodName);

    assertLockCount(2);
    assertTrue(checkForLocks(declaredLockDefs));
  }

  public void testAutolockAndNamedLock() throws Exception {
    config.addIncludePattern(this.targetClassName);
    String methodName = "internalSynchronizedInstanceMethod";
    // set up locks
    String methodExpression = "void " + targetClassName + "." + methodName + "()";
    LockDefinition ldnamed = new LockDefinitionImpl("test-lock", ConfigLockLevel.WRITE);
    ldnamed.commit();
    LockDefinition ldautolock = new LockDefinitionImpl(LockDefinition.TC_AUTOLOCK_NAME, ConfigLockLevel.WRITE);
    ldautolock.commit();

    config.getOrCreateSpec(targetClassName);
    // config.addLock(methodExpression, ldautolock);
    config.addLock(methodExpression, ldnamed);
    config.addLock(methodExpression, ldautolock);

    this.testClientObjectManager.setIsManaged(true);

    assertNoTransactions();

    invokeWithNoArgs(methodName);

    assertTrue(checkForLockName(ldnamed.getLockName(), ldnamed.getLockLevelAsInt()));
    assertTrue(checkForLock(ldnamed));
    assertAutolockCount(1);
    assertLockCount(2);
    assertNoAutolockLiteral();
  }

  public void testSynchronizedInstanceMethodWithAutolockAndNamedLock() throws Exception {
    config.addIncludePattern(this.targetClassName);
    String methodName = "synchronizedInstanceMethod";
    // set up locks
    String methodExpression = "void " + targetClassName + "." + methodName + "()";
    LockDefinition ldnamed = new LockDefinitionImpl("test-lock", ConfigLockLevel.WRITE);
    ldnamed.commit();
    LockDefinition ldautolock = new LockDefinitionImpl(LockDefinition.TC_AUTOLOCK_NAME, ConfigLockLevel.WRITE);
    ldautolock.commit();

    config.getOrCreateSpec(targetClassName);
    config.addLock(methodExpression, ldautolock);
    config.addLock(methodExpression, ldnamed);

    this.testClientObjectManager.setIsManaged(true);

    assertNoTransactions();

    invokeWithNoArgs(methodName);

    assertTrue(checkForLockName(ldnamed.getLockName(), ldnamed.getLockLevelAsInt()));
    assertTrue(checkForLock(ldnamed));
    assertAutolockCount(1);
    assertLockCount(2);
    assertNoAutolockLiteral();
  }

  public void testSerializationFields() throws Exception {
    config.getOrCreateSpec(targetClassName);
    Class notAdapted = Class.forName(targetClassName);
    Class adapted = classLoader.loadClass(targetClassName);
    assertNotSame(notAdapted, adapted);

    Field f = notAdapted.getDeclaredField("serialVersionUID");
    int mods = f.getModifiers();
    assertTrue(Modifier.isPrivate(mods));
    assertTrue(Modifier.isStatic(mods));
    assertTrue(Modifier.isFinal(mods));
    assertTrue(f.getType().equals(Long.TYPE));

    f = adapted.getDeclaredField("serialVersionUID");
    mods = f.getModifiers();
    assertTrue(Modifier.isPrivate(mods));
    assertTrue(Modifier.isStatic(mods));
    assertTrue(Modifier.isFinal(mods));
    assertTrue(f.getType().equals(Long.TYPE));

    f = notAdapted.getDeclaredField("serialPersistentFields");
    mods = f.getModifiers();
    assertTrue(Modifier.isPrivate(mods));
    assertTrue(Modifier.isStatic(mods));
    assertTrue(Modifier.isFinal(mods));
    assertTrue(f.getType().equals(ObjectStreamField[].class));

    f = adapted.getDeclaredField("serialPersistentFields");
    mods = f.getModifiers();
    assertTrue(Modifier.isPrivate(mods));
    assertTrue(Modifier.isStatic(mods));
    assertTrue(Modifier.isFinal(mods));
    assertTrue(f.getType().equals(ObjectStreamField[].class));
  }

  public void testMultipleCustomClassAdapters() throws Exception {
    TransparencyClassSpec spec = config.getOrCreateSpec(targetClassName);
    spec.addCustomClassAdapter(CustomClassAdapter1.FACTORY);
    spec.addCustomClassAdapter(CustomClassAdapter2.FACTORY);
    spec.addCustomClassAdapter(CustomClassAdapter3.FACTORY);

    Class clazz = this.classLoader.loadClass(targetClassName);
    Object o = clazz.newInstance();
    Method m = clazz.getDeclaredMethod("instanceMethodReturnsAValue", new Class[0]);
    Object result = m.invoke(o, new Object[0]);
    assertEquals("some return value & text added by first, second and third class adapter", result);
  }
  
  static class CustomClassAdapter1 extends ClassAdapter {
    public static final ClassAdapterFactory FACTORY = new ClassAdapterFactory() {
      public ClassAdapter create(ClassVisitor classvisitor, ClassLoader classloader) {
        return new CustomClassAdapter1(classvisitor);
      }
    };

    public CustomClassAdapter1(ClassVisitor visitor) {
      super(visitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      if ("instanceMethodReturnsAValue".equals(name)) {
        return new CustomMethodAdapter1(super.visitMethod(access, name, desc, signature, exceptions));
      } else {
        return super.visitMethod(access, name, desc, signature, exceptions);
      }
    }
    
    static class CustomMethodAdapter1 extends com.tc.asm.MethodAdapter implements Opcodes {
      public CustomMethodAdapter1(MethodVisitor mv) {
        super(mv);
      }

      @Override
      public void visitLdcInsn(Object cst) {
        if ("some return value".equals(cst)) {
          cst = cst + " & text added by first class adapter";
        }
        super.visitLdcInsn(cst);
      }
    }
  }
  
  static class CustomClassAdapter2 extends ClassAdapter {
    public static final ClassAdapterFactory FACTORY = new ClassAdapterFactory() {
      public ClassAdapter create(ClassVisitor classvisitor, ClassLoader classloader) {
        return new CustomClassAdapter2(classvisitor);
      }
    };

    public CustomClassAdapter2(ClassVisitor visitor) {
      super(visitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      if ("instanceMethodReturnsAValue".equals(name)) {
        return new CustomMethodAdapter2(super.visitMethod(access, name, desc, signature, exceptions));
      } else {
        return super.visitMethod(access, name, desc, signature, exceptions);
      }
    }
    
    static class CustomMethodAdapter2 extends com.tc.asm.MethodAdapter implements Opcodes {
      public CustomMethodAdapter2(MethodVisitor mv) {
        super(mv);
      }

      @Override
      public void visitLdcInsn(Object cst) {
        if (String.valueOf(cst).endsWith("first class adapter")) {
          cst = String.valueOf(cst).replaceAll("first class adapter", "first and second class adapter");
        }
        super.visitLdcInsn(cst);
      }
    }
  }
  
  static class CustomClassAdapter3 extends ClassAdapter {
    public static final ClassAdapterFactory FACTORY = new ClassAdapterFactory() {
      public ClassAdapter create(ClassVisitor classvisitor, ClassLoader classloader) {
        return new CustomClassAdapter3(classvisitor);
      }
    };

    public CustomClassAdapter3(ClassVisitor visitor) {
      super(visitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      if ("instanceMethodReturnsAValue".equals(name)) {
        return new CustomMethodAdapter3(super.visitMethod(access, name, desc, signature, exceptions));
      } else {
        return super.visitMethod(access, name, desc, signature, exceptions);
      }
    }
    
    static class CustomMethodAdapter3 extends com.tc.asm.MethodAdapter implements Opcodes {
      public CustomMethodAdapter3(MethodVisitor mv) {
        super(mv);
      }

      @Override
      public void visitLdcInsn(Object cst) {
        if (String.valueOf(cst).contains("first and second")) {
          cst = String.valueOf(cst).replaceAll("first and second", "first, second and third");
        }
        super.visitLdcInsn(cst);
      }
    }
  }

  private Object invokeWithDefaultArgs(String methodName) throws Exception {
    return invokeWithArgs(methodName, WITH_ARGS_PARAMS, WITH_ARGS_ARGS);
  }

  private Object invokeWithArgs(String methodName, Class[] ptypes, Object[] args) throws Exception {
    Class c = classLoader.loadClass(targetClassName);
    setTargetClientObjectManager(c);
    Object instance = c.newInstance();

    if (instance instanceof Manageable) {
      if (testClientObjectManager.isManaged(instance)) {
        ((Manageable) instance).__tc_managed(testClientObjectManager.lookupOrCreate(instance));
      }
    }

    boolean failOnException = false;
    return invokeMethod(c, instance, methodName, ptypes, args, failOnException);
  }

  private void invokeWithDefaultArgsAndCheckForProperException(String methodName) throws Exception {
    invokeWithArgsAndCheckForProperException(methodName, WITH_ARGS_PARAMS, WITH_ARGS_ARGS);
  }

  private void invokeWithArgsAndCheckForProperException(String methodName, Class[] ptypes, Object[] args)
      throws Exception {
    try {
      invokeWithArgs(methodName, ptypes, args);
      fail("Should have thrown an exception");
    } catch (InvocationTargetException e) {
      // expected
      assertExceptionType(e);
    }
  }

  private Object invokeWithNoArgs(String methodName) throws Exception {
    return invokeWithArgs(methodName, new Class[] {}, new Object[] {});
  }

  private void invokeWithNoArgsAndCheckForProperException(String methodName) throws Exception {
    try {
      invokeWithNoArgs(methodName);
      fail("Should have thrown an exception.");
    } catch (InvocationTargetException e) {
      // expected
      assertExceptionType(e);
    }
  }

  /**
   * Adds and
   */
  private void checkWithNamedLock(String methodName, boolean withArgs, boolean checkForException,
                                  String modifiersPattern, String parametersPattern) throws Exception {
    initializeConfig();
    createNamedLockDefinition("testLock");
    createLockConfigurationForMethodExpression(modifiersPattern, methodName, parametersPattern);

    initClassLoader();

    assertNoTransactions();
    if (withArgs) {
      if (checkForException) {
        invokeWithDefaultArgsAndCheckForProperException(methodName);
      } else {
        invokeWithDefaultArgs(methodName);
      }
    } else {
      if (checkForException) {
        invokeWithNoArgsAndCheckForProperException(methodName);
      } else {
        invokeWithNoArgs(methodName);
      }
    }
    assertNoAutolocks();
    assertNamedLockConditionsPostInvocation(1);
  }

  private void checkWithNamedLockNoArgs(String methodName, String modifiersPattern, String parametersPattern)
      throws Exception {
    checkWithNamedLock(methodName, false, false, modifiersPattern, parametersPattern);
  }

  private void checkWithNamedLockDefaultArgs(String methodName, String modifiersPattern, String parametersPattern)
      throws Exception {
    checkWithNamedLock(methodName, true, false, modifiersPattern, parametersPattern);
  }

  private void checkWithNamedLockNoArgsThrowsException(String methodName, String modifiersPattern,
                                                       String parametersPattern) throws Exception {
    checkWithNamedLock(methodName, false, true, modifiersPattern, parametersPattern);
  }

  private void checkWithnamedLockDefaultArgsThrowsException(String methodName, String modifiersPattern,
                                                            String parametersPattern) throws Exception {
    checkWithNamedLock(methodName, true, true, modifiersPattern, parametersPattern);
  }

  private void assertAutolockConditionsPostInvocation(int expectedTransactionCount) {
    assertTrue("Transaction count " + getLockCount() + " should be greater than or equal to"
               + " expected autolocks: " + expectedTransactionCount, getLockCount() >= expectedTransactionCount);
    assertAutolockCount(expectedTransactionCount);
    assertLockCount(expectedTransactionCount);
    assertNoAutolockLiteral();
  }

  private void assertNamedLockConditionsPostInvocation(int expectedTransactionCount) {
    assertNamedLockConditionsPostInvocation(expectedTransactionCount, new LockDefinition[] { this.lockDefinition });
  }

  private void assertNamedLockConditionsPostInvocation(int expectedTransactionCount, LockDefinition[] lockDefs) {
    for (int i = 0; i < lockDefs.length; i++) {
      assertTrue(checkForLockName(lockDefs[i].getLockName(), lockDefs[i].getLockLevelAsInt()));
      assertTrue(checkForLock(lockDefs[i]));
    }
    assertLockCount(expectedTransactionCount);
  }

  private void assertExceptionType(InvocationTargetException e) {
    Throwable target = e.getTargetException();
    // Must compare names, since the classes aren't the same (different
    // classloaders).
    assertEquals(LockTestThrowsExceptionException.class.getName(), target.getClass().getName());
  }

  private void assertLockCount(int transactionCount) {
    assertEquals(transactionCount, getLockCount());
    assertEquals(transactionCount, testLockManager.getUnlockCount());
  }

  private int getLockCount() {
    return testLockManager.getBegins().size();
  }

  private void assertAutolockCount(int autolockCount) {
    assertEquals(autolockCount, getAutolockBeginCount());
  }

  private void assertNoTransactions() {
    assertLockCount(0);
  }

  private void assertNoAutolocks() {
    assertAutolockCount(0);
    assertNoAutolockLiteral();
  }

  private int getAutolockBeginCount() {
    int rv = 0;
    for (Iterator i = testLockManager.getBegins().iterator(); i.hasNext();) {
      Begin b = (Begin) i.next();
      // hack
      if ((b.lock instanceof DsoLockID) || (b.lock instanceof DsoLiteralLockID)) {
        rv++;
      }
      if ((b.lock instanceof StringLockID) && (!((StringLockID) b.lock).asString().startsWith("^"))) {
        rv++;
      }
    }
    return rv;
  }

  private boolean checkForLocks(LockDefinition[] lockDefs) {
    for (int i = 0; i < lockDefs.length; i++) {
      if (!checkForLock(lockDefs[i])) return false;
    }
    return true;
  }

  private boolean checkForLock(LockDefinition lockdef) {
    return checkForLock(lockdef, this.testLockManager.getBegins());
  }

  /**
   * Returns true if the lock name of the LockDefinition is found in the List. The List should be a list of String
   * arrays, each corresponding to a call to TransactionManager.begin(String[] locks)
   */
  private boolean checkForLock(LockDefinition lockdef, List beginTransactions) {
    boolean rv = false;
    for (Iterator iter = beginTransactions.iterator(); iter.hasNext();) {
      rv = checkForLock(lockdef, (Begin) iter.next());
      if (rv) break;
    }
    return rv;
  }

  /**
   * Returns true if the lock name of the LockDefinition is found in the String array.
   */
  private boolean checkForLock(LockDefinition lockdef, Begin lock) {
    boolean rv = false;
    if (lock != null) {
      if (lockdef.isAutolock()) {
        rv = (lock.lock instanceof DsoLiteralLockID);
      } else {
        rv = new StringLockID("^" + lockdef.getLockName()).equals(lock.lock);
      }
      if (rv) {
        // make sure that the lock type is the same
        rv = checkLockType(lockdef, lock.level.toInt());
      }
    }
    return rv;
  }

  private boolean checkForLockName(String lockName, int lockType) {
    List begins = this.testLockManager.getBegins();
    for (Iterator i = begins.iterator(); i.hasNext();) {
      Begin lock = (Begin) i.next();
      if (checkForLockName(lockName, lock)) return true;
    }
    return false;
  }

  private static boolean checkForLockName(String lockName, Begin lock) {
    if (LockDefinition.TC_AUTOLOCK_NAME.equals(lockName)) {
      return (lock.lock instanceof DsoLiteralLockID);
    } else {
      return (new StringLockID("^" + lockName)).equals(lock.lock);
    }    
  }

  /**
   * Check the string representing the actual lock used to make sure that it is the same type
   */
  private boolean checkLockType(LockDefinition lockdef, int lockType) {
    return (lockdef.getLockLevelAsInt() == lockType);
  }

  private void assertNoAutolockLiteral() {
    assertFalse(checkForLockName(LockDefinition.TC_AUTOLOCK_NAME, LockLevel.WRITE.toInt()));
  }

  private void createNamedLockDefinition(String lockName) {
    this.config.addIncludePattern(this.targetClassName);
    this.lockDefinition = new LockDefinitionImpl(lockName, ConfigLockLevel.WRITE);
    this.lockDefinition.commit();
  }

  private void createLockConfigurationForMethodExpression(String modifiersPattern, String testMethodName,
                                                          String parameterPattern) {
    String methodExpression = modifiersPattern + " " + targetClassName + "." + testMethodName + parameterPattern;

    config.getOrCreateSpec(targetClassName);
    config.addLock(methodExpression, lockDefinition);
  }

  private Object callNoArgCtor() throws Exception {
    // we're going to be clearing the counts, so make sure the caller assumes no TXNs to start with
    assertNoTransactions();

    // this makes sure the class initializer is called
    Class c = Class.forName(targetClassName, true, this.classLoader);
    setTargetClientObjectManager(c);

    // depending on the compiler (eclipse vs. javac, there might methods called that obtain locks)
    this.testLockManager.resetCounts();

    Constructor ctor = c.getConstructor(new Class[] {});
    try {
      return ctor.newInstance(new Object[] {});
    } catch (InvocationTargetException ite) {
      Throwable t = ite.getTargetException();
      if (t instanceof RuntimeException) { throw (RuntimeException) t; }
      if (t instanceof Error) { throw (Error) t; }
      throw new RuntimeException(t);
    }
  }

}
