/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect.impl.java;

import com.tc.backport175.bytecode.AnnotationElement;


import java.lang.reflect.Field;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.ReflectHelper;
import com.tc.aspectwerkz.reflect.FieldInfo;

/**
 * Implementation of the FieldInfo interface for java.lang.reflect.*.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class JavaFieldInfo extends JavaMemberInfo implements FieldInfo {
  /**
   * The field type.
   */
  private ClassInfo m_type = null;

  /**
   * The signature of the field.
   */
  private final String m_signature;

  /**
   * Creates a new field java instance.
   *
   * @param field
   * @param declaringType
   */
  JavaFieldInfo(final Field field, final JavaClassInfo declaringType) {
    super(field, declaringType);
    m_signature = ReflectHelper.getFieldSignature(field);
  }

  /**
   * Returns the field info for the field specified.
   *
   * @param field the field
   * @return the field info
   */
  public static FieldInfo getFieldInfo(final Field field) {
    Class declaringClass = field.getDeclaringClass();
    JavaClassInfoRepository repository = JavaClassInfoRepository.getRepository(declaringClass.getClassLoader());
    ClassInfo classInfo = repository.getClassInfo(declaringClass.getName());
    if (classInfo == null) {
      classInfo = JavaClassInfo.getClassInfo(declaringClass);
    }
    return classInfo.getField(ReflectHelper.calculateHash(field));
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
    return getDeclaringType().getAnnotationReader().getFieldAnnotationElements(getName(), m_signature);
  }

  /**
   * Returns the type.
   *
   * @return the type
   */
  public synchronized ClassInfo getType() {
    if (m_type == null) {
      Class type = ((Field) m_member).getType();
      if (m_classInfoRepository.hasClassInfo(type.getName())) {
        m_type = m_classInfoRepository.getClassInfo(type.getName());
      } else {
        m_type = JavaClassInfo.getClassInfo(type);
        m_classInfoRepository.addClassInfo(m_type);
      }
    }
    return m_type;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FieldInfo)) {
      return false;
    }
    FieldInfo fieldInfo = (FieldInfo) o;
    if (!m_declaringType.getName().equals(fieldInfo.getDeclaringType().getName())) {
      return false;
    }
    if (!m_member.getName().equals(fieldInfo.getName())) {
      return false;
    }
    ClassInfo fieldType = fieldInfo.getType();
    if (!m_type.getName().equals(fieldType.getName())) {
      return false;
    }
    return true;
  }

  public int hashCode() {
    int result = 29;
    if (m_type == null) {
      getType();
    }
    result = (29 * result) + m_declaringType.getName().hashCode();
    result = (29 * result) + m_member.getName().hashCode();
    result = (29 * result) + getType().getName().hashCode();
    return result;
  }
}