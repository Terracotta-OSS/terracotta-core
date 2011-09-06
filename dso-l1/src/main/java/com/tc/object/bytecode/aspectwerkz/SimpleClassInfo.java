/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.aspectwerkz;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.ConstructorInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.StaticInitializationInfo;
import com.tc.backport175.bytecode.AnnotationElement;
import com.tc.backport175.bytecode.AnnotationReader;

public class SimpleClassInfo implements ClassInfo {

  private static final ClassInfo[] NO_INTERFACES = new ClassInfo[0];
  
  private String className;

  protected SimpleClassInfo(String className) {
    this.className = className;
  }

  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public ConstructorInfo getConstructor(int hash) {
    throw new RuntimeException();
  }

  public ConstructorInfo[] getConstructors() {
    throw new RuntimeException();
  }

  public MethodInfo getMethod(int hash) {
    throw new RuntimeException();
  }

  public MethodInfo[] getMethods() {
    throw new RuntimeException();
  }

  public FieldInfo getField(int hash) {
    throw new RuntimeException();
  }

  public FieldInfo[] getFields() {
    throw new RuntimeException();
  }

  public ClassLoader getClassLoader() {
    throw new RuntimeException();
  }

  public boolean hasStaticInitializer() {
    throw new RuntimeException();
  }

  public StaticInitializationInfo staticInitializer() {
    throw new RuntimeException();
  }

  public ClassInfo[] getInterfaces() {
    return NO_INTERFACES;
  }

  public ClassInfo getSuperclass() {
    return null;
  }

  public ClassInfo getComponentType() {
    throw new RuntimeException();
  }

  public boolean isInterface() {
    throw new RuntimeException();
  }

  public boolean isPrimitive() {
    throw new RuntimeException();
  }

  public boolean isArray() {
    throw new RuntimeException();
  }

  public AnnotationReader getAnnotationReader() {
    throw new RuntimeException();
  }

  public String getName() {
    return this.className;
  }

  public String getSignature() {
    throw new RuntimeException();
  }
  
  public String getGenericsSignature() {
    throw new RuntimeException();
  }

  public int getModifiers() {
    throw new RuntimeException();
  }

  public AnnotationElement.Annotation[] getAnnotations() {
    throw new RuntimeException();
  }

}
