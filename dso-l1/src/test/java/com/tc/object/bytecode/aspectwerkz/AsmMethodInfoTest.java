/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.aspectwerkz;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.impl.java.JavaMethodInfo;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.config.DSOClientConfigHelper;
import com.tctest.AsmMethodInfoTestHelper;

import java.lang.reflect.Method;

/**
 * Unit test for AsmMethodInfo
 */
public class AsmMethodInfoTest extends BaseDSOTestCase {

  AsmMethodInfo         methodInfo;
  MethodInfo            javaMethodInfo;
  DSOClientConfigHelper config;

  private void setUp(Method method) throws Exception {
    this.methodInfo = AsmMethodInfo.createNewAsmMethodInfo(method);
    javaMethodInfo = JavaMethodInfo.getMethodInfo(method);
    config = configHelper();
  }

  public void testAsmMethodInfoTestHelperTest4() throws Exception {
    Method method = AsmMethodInfoTestHelper.class.getMethod("test4", new Class[] { Integer.TYPE, Object.class });
    setUp(method);
    assertTrue(compareMethodInfos(this.javaMethodInfo, this.methodInfo));

    String expression = "long " + AsmMethodInfoTestHelper.class.getName() + "." + method.getName()
                        + "(int, java.lang.Object)";
    assertTrue(config.matches(expression, this.javaMethodInfo));
    assertTrue(config.matches(expression, this.methodInfo));
  }

  public void publicVoidTestMethod() throws Exception { /**/
  }

  public void testPublicVoidTestMethod() throws Exception {
    Method method = getClass().getMethod("publicVoidTestMethod", new Class[0]);
    setUp(method);
    assertTrue(compareMethodInfos(this.javaMethodInfo, this.methodInfo));
  }

  public void testPublicVoidTestMethodMatch() throws Exception {
    String methodName = "publicVoidTestMethod";
    Method method = getClass().getMethod(methodName, new Class[0]);
    setUp(method);
    String expression = "* " + getClass().getName() + "." + methodName + "()";
    assertTrue(config.matches(expression, this.javaMethodInfo));
    assertTrue(config.matches(expression, this.methodInfo));
  }

  public void publicVoidTestMethodStringInt(String string, int i) throws Exception { /**/
  }

  public void testPublicVoidTestMethodStringInt() throws Exception {
    Method method = getClass().getMethod("publicVoidTestMethodStringInt", new Class[] { String.class, Integer.TYPE });
    setUp(method);
    assertTrue(compareMethodInfos(this.javaMethodInfo, this.methodInfo));
  }

  public long[][][] publicLong3TestMethodDouble4(double[][][][] double4) throws Exception {
    return null;
  }

  public void testPublicLong3TestMethodDouble4() throws Exception {
    Method method = getClass().getMethod("publicLong3TestMethodDouble4", new Class[] { double[][][][].class });
    setUp(method);
    assertTrue(compareMethodInfos(this.javaMethodInfo, this.methodInfo));

  }

  private static boolean compareMethodInfos(MethodInfo source, MethodInfo target) {
    // test the method name;
    // System.out.println("Testing method name: " + source.getName() + ", " +
    // target.getName());
    if (source.getModifiers() != target.getModifiers()) { return false; }
    if (!source.getName().equals(target.getName())) { return false; }
    // test the class name
    if (!compareClassInfos(source.getDeclaringType(), target.getDeclaringType())) { return false; }
    // test the return type
    if (!compareClassInfos(source.getReturnType(), target.getReturnType())) { return false; }
    // test the parameter types
    if (!(source.getParameterTypes().length == target.getParameterTypes().length)) { return false; }
    for (int i = 0; i < source.getParameterTypes().length; i++) {
      if (!compareClassInfos(source.getParameterTypes()[i], target.getParameterTypes()[i])) { return false; }
    }
    if (!(source.getExceptionTypes().length == target.getExceptionTypes().length)) { return false; }
    // XXX: If the exceptions aren't reported in the same order, this test will
    // fail, which
    // is probably wrong.
    for (int i = 0; i < source.getExceptionTypes().length; i++) {
      if (!compareClassInfos(source.getExceptionTypes()[i], target.getExceptionTypes()[i])) { return false; }
    }
    return true;
  }

  private static boolean compareClassInfos(ClassInfo source, ClassInfo target) {
    // System.out.println("source name: "+source.getName() + ", target name: " +
    // target.getName());
    return source.getName().equals(target.getName());
  }

}