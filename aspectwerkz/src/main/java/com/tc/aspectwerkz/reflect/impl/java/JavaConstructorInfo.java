/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect.impl.java;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.ConstructorInfo;
import com.tc.aspectwerkz.reflect.ReflectHelper;
import com.tc.backport175.bytecode.AnnotationElement;

import java.lang.reflect.Constructor;

/**
 * Implementation of the ConstructorInfo interface for java.lang.reflect.*.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class JavaConstructorInfo extends JavaMemberInfo implements ConstructorInfo {
  /**
   * A list with the parameter types.
   */
  private ClassInfo[] m_parameterTypes = null;

  /**
   * A list with the exception types.
   */
  private ClassInfo[] m_exceptionTypes = null;

  /**
   * The signature of the class.
   */
  private final String m_signature;

  /**
   * Creates a new method meta data instance.
   *
   * @param constructor
   * @param declaringType
   */
  JavaConstructorInfo(final Constructor constructor, final JavaClassInfo declaringType) {
    super(constructor, declaringType);
    m_signature = ReflectHelper.getConstructorSignature(constructor);
  }

  /**
   * Returns the constructor info for the constructor specified.
   *
   * @param constructor the constructor
   * @return the constructor info
   */
  public static ConstructorInfo getConstructorInfo(final Constructor constructor) {
    Class declaringClass = constructor.getDeclaringClass();
    JavaClassInfoRepository repository = JavaClassInfoRepository.getRepository(declaringClass.getClassLoader());
    ClassInfo classInfo = repository.getClassInfo(declaringClass.getName());
    if (classInfo == null) {
      classInfo = JavaClassInfo.getClassInfo(declaringClass);
    }
    return classInfo.getConstructor(ReflectHelper.calculateHash(constructor));
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
   * Returns the attributes.
   *
   * @return the attributes
   */
  public AnnotationElement.Annotation[] getAnnotations() {
    return getDeclaringType().getAnnotationReader().getConstructorAnnotationElements(m_signature);
  }

  /**
   * Returns the parameter types.
   *
   * @return the parameter types
   */
  public synchronized ClassInfo[] getParameterTypes() {
    if (m_parameterTypes == null) {
      Class[] parameterTypes = ((Constructor) m_member).getParameterTypes();
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
   * Returns the exception types.
   *
   * @return the exception types
   */
  public synchronized ClassInfo[] getExceptionTypes() {
    if (m_exceptionTypes == null) {
      Class[] exceptionTypes = ((Constructor) m_member).getExceptionTypes();
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
    if (!(o instanceof ConstructorInfo)) {
      return false;
    }
    ConstructorInfo constructorInfo = (ConstructorInfo) o;
    if (!m_declaringType.getName().equals(constructorInfo.getDeclaringType().getName())) {
      return false;
    }
    if (!m_member.getName().equals(constructorInfo.getName())) {
      return false;
    }
    Class[] parameterTypes1 = ((Constructor) m_member).getParameterTypes();
    ClassInfo[] parameterTypes2 = constructorInfo.getParameterTypes();
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
    Class[] parameterTypes = ((Constructor) m_member).getParameterTypes();
    for (int i = 0; i < parameterTypes.length; i++) {
      result = (29 * result) + parameterTypes[i].getName().hashCode();
    }
    return result;
  }
}