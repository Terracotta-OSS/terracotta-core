/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect;

import com.tc.backport175.bytecode.AnnotationElement;
import com.tc.backport175.bytecode.AnnotationReader;

public class NullClassInfo implements ClassInfo {

  private String name = "__UNKNOWN__";

  public NullClassInfo() {
    //
  }

  public ConstructorInfo getConstructor(int hash) {
    return null;
  }

  public ConstructorInfo[] getConstructors() {
    return new ConstructorInfo[0];
  }

  public MethodInfo getMethod(int hash) {
    return null;
  }

  public MethodInfo[] getMethods() {
    return new MethodInfo[0];
  }

  public FieldInfo getField(int hash) {
    return null;
  }

  public FieldInfo[] getFields() {
    return new FieldInfo[0];
  }

  public boolean hasStaticInitializer() {
    return false;
  }

  /**
   * @see com.tc.aspectwerkz.reflect.ClassInfo#staticInitializer()
   */
  public StaticInitializationInfo staticInitializer() {
    return null;
  }

  public ClassInfo[] getInterfaces() {
    return new ClassInfo[0];
  }

  public ClassInfo getSuperclass() {
    return null;
  }

  public ClassLoader getClassLoader() {
    return null;
  }

  public ClassInfo getComponentType() {
    return null;
  }

  public boolean isInterface() {
    return false;
  }

  public boolean isPrimitive() {
    return false;
  }

  public boolean isArray() {
    return false;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSignature() {
    return null;
  }

  public String getGenericsSignature() {
    return null;
  }

  public int getModifiers() {
    return 0;
  }

  public AnnotationElement.Annotation[] getAnnotations() {
    return ClassInfo.EMPTY_ANNOTATION_ARRAY;
  }

  public AnnotationReader getAnnotationReader() {
    return null;
  }
}