/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.aspectwerkz;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.tc.asm.Type;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.backport175.bytecode.AnnotationElement;
import com.tc.exception.ImplementMe;

import java.lang.reflect.Method;

/**
 * Converts Asm method descriptions to Aspectwerkz MethodInfo
 */
public class AsmMethodInfo implements MethodInfo {
  private int              modifiers;
  private ClassInfo        declaringType;
  private String           name;
  private ClassInfo        returnTypeInfo;
  private ClassInfo[]      parameterTypeInfos;
  private ClassInfo[]      exceptionTypeInfos;
  private ClassInfoFactory classInfoFactory;

  public AsmMethodInfo(ClassInfoFactory classInfoFactory, int modifiers, String className, String methodName,
                       String desc, String[] exceptions) {
    this.classInfoFactory = classInfoFactory;
    // handle modifiers
    this.modifiers = modifiers;
    // handle declaring type
    this.declaringType = classInfoFactory.getClassInfo(className);//new SimpleClassInfo(className);
    // handle method name
    this.name = methodName.equals("<init>") ? "__INIT__" : methodName;
    // handle return type.
    this.returnTypeInfo = type2ClassInfo(Type.getReturnType(desc));
    // handle parameter types
    Type[] parameterTypes = Type.getArgumentTypes(desc);
    this.parameterTypeInfos = types2ClassInfos(parameterTypes);
    // handle exception types
    this.exceptionTypeInfos = classNames2ClassInfos(exceptions);
  }

  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  private ClassInfo[] classNames2ClassInfos(String[] classNames) {
    if (classNames == null) return null;
    ClassInfo[] rv = new ClassInfo[classNames.length];
    for (int i = 0; i < classNames.length; i++) {
      rv[i] = className2ClassInfo(classNames[i]);
    }
    return rv;
  }

  private ClassInfo[] types2ClassInfos(Type[] types) {
    if (types == null) return null;
    ClassInfo[] rv = new ClassInfo[types.length];
    for (int i = 0; i < types.length; i++) {
      rv[i] = type2ClassInfo(types[i]);
    }
    return rv;
  }

  private ClassInfo className2ClassInfo(String className) {
    return classInfoFactory.getClassInfo(className);//new SimpleClassInfo(className);
  }

  private ClassInfo type2ClassInfo(Type type) {
    return className2ClassInfo(type.getClassName());
  }

  public ClassInfo getReturnType() {
    return this.returnTypeInfo;
  }

  public ClassInfo[] getParameterTypes() {
    return this.parameterTypeInfos;
  }

  public String[] getParameterNames() {
    return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  public ClassInfo[] getExceptionTypes() {
    return this.exceptionTypeInfos;
  }

  public ClassInfo getDeclaringType() {
    return this.declaringType;
  }

  public String getName() {
    return this.name;
  }

  public String getSignature() {
    return null;
  }
  
  public String getGenericsSignature() {
    return null;
  }

  public int getModifiers() {
    return modifiers;
  }

  public AnnotationElement.Annotation[] getAnnotations() {
    throw new ImplementMe();
  }

  /**
   * Creates a new AsmMethodInfo from the given Method. This is only used for testing.
   */
  public static AsmMethodInfo createNewAsmMethodInfo(Method method) {
    int modifiers = method.getModifiers();
    String className = method.getDeclaringClass().getName();
    String methodName = method.getName();
    String desc = Type.getMethodDescriptor(method);
    Class[] exceptionTypes = method.getExceptionTypes();
    String[] exceptionTypeNames = new String[exceptionTypes.length];
    for (int i = 0; i < exceptionTypes.length; i++) {
      exceptionTypeNames[i] = exceptionTypes[i].getName();
    }
    return new AsmMethodInfo(new ClassInfoFactory(), modifiers, className, methodName, desc, exceptionTypeNames);
  }

}