/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect.impl.java;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.ReflectHelper;
import com.tc.backport175.bytecode.AnnotationElement;

import java.lang.reflect.Method;

/**
 * Implementation of the MethodInfo interface for java.lang.reflect.*.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class JavaMethodInfo extends JavaMemberInfo implements MethodInfo {

  /**
   * The return type.
   */
  private ClassInfo m_returnType = null;

  /**
   * A list with the parameter types.
   */
  private ClassInfo[] m_parameterTypes = null;

  /**
   * A list with the exception types.
   */
  private ClassInfo[] m_exceptionTypes = null;

  /**
   * The signature of the method.
   */
  private final String m_signature;

  /**
   * Creates a new method meta data instance.
   *
   * @param method
   * @param declaringType
   */
  JavaMethodInfo(final Method method, final JavaClassInfo declaringType) {
    super(method, declaringType);
    m_signature = ReflectHelper.getMethodSignature(method);
  }

  /**
   * Returns the method info for the method specified.
   *
   * @param method the method
   * @return the method info
   */
  public static MethodInfo getMethodInfo(final Method method) {
    Class declaringClass = method.getDeclaringClass();
    JavaClassInfoRepository repository = JavaClassInfoRepository.getRepository(declaringClass.getClassLoader());
    ClassInfo classInfo = repository.getClassInfo(declaringClass.getName());
    if (classInfo == null) {
      classInfo = JavaClassInfo.getClassInfo(declaringClass);
    }
    return classInfo.getMethod(ReflectHelper.calculateHash(method));
  }

  /**
   * Returns the signature for the element.
   *
   * @return the signature for the element
   */
  public String getSignature() {
    return m_signature;
  }

  public String getGenericsSignature() {
    // XXX implement
    throw new RuntimeException();
  }

  /**
   * Returns the annotations.
   *
   * @return the annotations
   */
  public AnnotationElement.Annotation[] getAnnotations() {
    return getDeclaringType().getAnnotationReader().getMethodAnnotationElements(m_member.getName(), m_signature);
  }

  /**
   * Returns the return type.
   *
   * @return the return type
   */
  public synchronized ClassInfo getReturnType() {
    if (m_returnType == null) {
      Class returnTypeClass = ((Method) m_member).getReturnType();
      if (m_classInfoRepository.hasClassInfo(returnTypeClass.getName())) {
        m_returnType = m_classInfoRepository.getClassInfo(returnTypeClass.getName());
      } else {
        m_returnType = JavaClassInfo.getClassInfo(returnTypeClass);
        m_classInfoRepository.addClassInfo(m_returnType);
      }
    }
    return m_returnType;
  }

  /**
   * Returns the parameter types.
   *
   * @return the parameter types
   */
  public synchronized ClassInfo[] getParameterTypes() {
    if (m_parameterTypes == null) {
      Class[] parameterTypes = ((Method) m_member).getParameterTypes();
      m_parameterTypes = new ClassInfo[parameterTypes.length];
      for (int i = 0; i < parameterTypes.length; i++) {
        Class parameterType = parameterTypes[i];
        ClassInfo metaData;
        if (m_classInfoRepository.hasClassInfo(parameterType.getName())) {
          metaData = m_classInfoRepository.getClassInfo(parameterType.getName());
        } else {
          metaData = JavaClassInfo.getClassInfo(parameterType);
          m_classInfoRepository.addClassInfo(metaData);
        }
        m_parameterTypes[i] = metaData;
      }
    }
    return m_parameterTypes;
  }

  /**
   * Returns the parameter names as they appear in the source code.
   * <p/>
   * This information is not available from Reflect.
   * We may use ASM to grab it - is that needed ?
   *
   * @return null / not supported for now.
   */
  public String[] getParameterNames() {
    return null;
  }

  /**
   * Returns the exception types.
   *
   * @return the exception types
   */
  public synchronized ClassInfo[] getExceptionTypes() {
    if (m_exceptionTypes == null) {
      Class[] exceptionTypes = ((Method) m_member).getExceptionTypes();
      m_exceptionTypes = new ClassInfo[exceptionTypes.length];
      for (int i = 0; i < exceptionTypes.length; i++) {
        Class exceptionType = exceptionTypes[i];
        ClassInfo metaData;
        if (m_classInfoRepository.hasClassInfo(exceptionType.getName())) {
          metaData = m_classInfoRepository.getClassInfo(exceptionType.getName());
        } else {
          metaData = JavaClassInfo.getClassInfo(exceptionType);
          m_classInfoRepository.addClassInfo(metaData);
        }
        m_exceptionTypes[i] = metaData;
      }
    }
    return m_exceptionTypes;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MethodInfo)) {
      return false;
    }
    MethodInfo methodInfo = (MethodInfo) o;
    if (!m_declaringType.getName().equals(methodInfo.getDeclaringType().getName())) {
      return false;
    }
    if (!m_member.getName().equals(methodInfo.getName())) {
      return false;
    }
    Class[] parameterTypes1 = ((Method) m_member).getParameterTypes();
    ClassInfo[] parameterTypes2 = methodInfo.getParameterTypes();
    if (parameterTypes1.length != parameterTypes2.length) {
      return false;
    }
    for (int i = 0; i < parameterTypes1.length; i++) {
      if (!parameterTypes1[i].getName().equals(parameterTypes2[i].getName())) {
        return false;
      }
    }
    return true;
  }

  public int hashCode() {
    int result = 29;
    result = (29 * result) + m_declaringType.getName().hashCode();
    result = (29 * result) + m_member.getName().hashCode();
    Class[] parameterTypes = ((Method) m_member).getParameterTypes();
    for (int i = 0; i < parameterTypes.length; i++) {
      result = (29 * result) + parameterTypes[i].getName().hashCode();
    }
    return result;
  }

  public String toString() {
    return m_member.toString();
  }
}