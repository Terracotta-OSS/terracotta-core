/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.aspectwerkz;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.backport175.bytecode.AnnotationElement.Annotation;
import com.tc.exception.ImplementMe;

public class SimpleMethodInfo implements MethodInfo {

  private String    methodName;
  private ClassInfo declaringType;
  private ClassInfo returnType;
  private int modifiers;
  private ClassInfo[] parameterTypes;
  private ClassInfo[] exceptionTypes;

  public SimpleMethodInfo(ClassInfo declaringType, String methodName, int modifiers, ClassInfo returnType,
                          ClassInfo[] parameterTypes, ClassInfo[] exceptionTypes) {
    setAll(declaringType, methodName, modifiers, returnType, parameterTypes, exceptionTypes);
  }

  private void setAll(ClassInfo declaringType, String methodName, int modifiers, ClassInfo returnType,
                      ClassInfo[] parameterTypes, ClassInfo[] exceptionTypes) {
    this.declaringType = declaringType;
    this.methodName = methodName;
    this.modifiers = modifiers;
    this.returnType = returnType;
    this.parameterTypes = parameterTypes;
    this.exceptionTypes = exceptionTypes;

  }

  public ClassInfo getReturnType() {
    return this.returnType;
  }

  public ClassInfo[] getParameterTypes() {
    return this.parameterTypes;
  }

  public String[] getParameterNames() {
    return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  public ClassInfo[] getExceptionTypes() {
    return this.exceptionTypes;
  }

  public ClassInfo getDeclaringType() {
    return this.declaringType;
  }

  public String getName() {
    return this.methodName;
  }

  public String getSignature() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
  
  public String getGenericsSignature() {
    return null;
  }

  public int getModifiers() {
    return this.modifiers;
  }

  public Annotation[] getAnnotations() {
    throw new ImplementMe();
  }
}