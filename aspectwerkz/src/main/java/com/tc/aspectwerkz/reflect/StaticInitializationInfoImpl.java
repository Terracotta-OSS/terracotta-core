/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.reflect;

import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.backport175.bytecode.AnnotationElement;
import com.tc.asm.Opcodes;


/**
 * Sole implementation of <CODE>StaticInitializationInfo</CODE>.
 *
 * @author <a href="mailto:the_mindstorm@evolva.ro">Alex Popescu</a>
 */
public class StaticInitializationInfoImpl implements StaticInitializationInfo {
  protected ClassInfo m_declaringType;

  public StaticInitializationInfoImpl(final ClassInfo classInfo) {
    m_declaringType = classInfo;
  }

  /**
   * @see org.codehaus.aspectwerkz.reflect.MemberInfo#getDeclaringType()
   */
  public ClassInfo getDeclaringType() {
    return m_declaringType;
  }

  /**
   * @see org.codehaus.aspectwerkz.reflect.ReflectionInfo#getName()
   */
  public String getName() {
    return TransformationConstants.CLINIT_METHOD_NAME;
  }

  /**
   * @see org.codehaus.aspectwerkz.reflect.ReflectionInfo#getSignature()
   */
  public String getSignature() {
    return TransformationConstants.CLINIT_METHOD_SIGNATURE;
  }
  
  public String getGenericsSignature() {
    return null;
  }

  /**
   * @see org.codehaus.aspectwerkz.reflect.ReflectionInfo#getModifiers()
   */
  public int getModifiers() {
    return Opcodes.ACC_STATIC;
  }

  /**
   * @see org.codehaus.aspectwerkz.reflect.ReflectionInfo#getAnnotations()
   */
  public AnnotationElement.Annotation[] getAnnotations() {
    return ClassInfo.EMPTY_ANNOTATION_ARRAY;
  }

}
