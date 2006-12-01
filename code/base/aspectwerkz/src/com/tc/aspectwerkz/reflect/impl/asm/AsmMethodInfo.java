/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect.impl.asm;

import com.tc.backport175.bytecode.AnnotationElement;
import com.tc.asm.Type;

import com.tc.aspectwerkz.exception.DefinitionException;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;

import java.lang.reflect.Modifier;

/**
 * ASM implementation of the MethodInfo interface.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class AsmMethodInfo extends AsmMemberInfo implements MethodInfo {

  /**
   * The return type name.
   */
  private String m_returnTypeName = null;

  /**
   * A list with the parameter names as they appear in the source code.
   * This information may not be available.
   */
  protected String[] m_parameterNames = null;

  /**
   * A list with the parameter type names.
   */
  private String[] m_parameterTypeNames = null;

  /**
   * A list with the exception type names.
   */
  private String[] m_exceptionTypeNames = null;

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
   * Creates a new method info instance.
   *
   * @param method
   * @param declaringType
   * @param loader
   */
  AsmMethodInfo(final MethodStruct method, final String declaringType, final ClassLoader loader) {
    super(method, declaringType, loader);

    m_returnTypeName = Type.getReturnType(method.desc).getClassName();
    Type[] argTypes = Type.getArgumentTypes(method.desc);
    m_parameterTypeNames = new String[argTypes.length];
    for (int i = 0; i < argTypes.length; i++) {
      m_parameterTypeNames[i] = argTypes[i].getClassName();
    }
    // TODO how to do exceptions?
    m_exceptionTypeNames = new String[]{};
  }

  /**
   * Returns the method info for the method specified.
   *
   * @param methodName
   * @param methodDesc
   * @param bytecode
   * @param loader
   * @return the method info
   */
  public static MethodInfo getMethodInfo(final String methodName,
                                         final String methodDesc,
                                         final byte[] bytecode,
                                         final ClassLoader loader) {
    String className = AsmClassInfo.retrieveClassNameFromBytecode(bytecode);
    AsmClassInfoRepository repository = AsmClassInfoRepository.getRepository(loader);
    ClassInfo classInfo = repository.getClassInfo(className);
    if (classInfo == null) {
      classInfo = AsmClassInfo.getClassInfo(bytecode, loader);
    }
    return classInfo.getMethod(AsmHelper.calculateMethodHash(methodName, methodDesc));
  }

  /**
   * Returns the signature for the element.
   *
   * @return the signature for the element
   */
  public String getSignature() {
    return AsmHelper.getMethodDescriptor(this);
  }
  
  public String getGenericsSignature() {
    return m_member.signature; 
  }

  /**
   * Returns the return type.
   *
   * @return the return type
   */
  public ClassInfo getReturnType() {
    if (m_returnType == null) {
      m_returnType = AsmClassInfo.getClassInfo(m_returnTypeName, (ClassLoader) m_loaderRef.get());
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
      m_parameterTypes = new ClassInfo[m_parameterTypeNames.length];
      for (int i = 0; i < m_parameterTypeNames.length; i++) {
        m_parameterTypes[i] = AsmClassInfo.getClassInfo(
                m_parameterTypeNames[i],
                (ClassLoader) m_loaderRef.get()
        );
      }
    }
    return m_parameterTypes;
  }

  /**
   * Returns the parameter names as they appear in the source code.
   * This information is available only when class are compiled with javac -g (debug info), but is required
   * for Aspect that are using args() and target()/this() bindings.
   * <p/>
   * It returns null if not available.
   *
   * @return
   */
  public String[] getParameterNames() {
    return m_parameterNames;
  }

  /**
   * Returns the exception types.
   *
   * @return the exception types
   */
  public synchronized ClassInfo[] getExceptionTypes() {
    if (m_exceptionTypes == null) {
      m_exceptionTypes = new ClassInfo[m_exceptionTypeNames.length];
      for (int i = 0; i < m_exceptionTypeNames.length; i++) {
        m_exceptionTypes[i] = AsmClassInfo.getClassInfo(
                m_exceptionTypeNames[i],
                (ClassLoader) m_loaderRef.get()
        );
      }
    }
    return m_exceptionTypes;
  }

  /**
   * Returns the annotations.
   *
   * @return the annotations
   */
  public AnnotationElement.Annotation[] getAnnotations() {
    return getDeclaringType().getAnnotationReader().getMethodAnnotationElements(m_member.name, m_member.desc);
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MethodInfo)) {
      return false;
    }
    MethodInfo methodInfo = (MethodInfo) o;
    if (!m_declaringTypeName.equals(methodInfo.getDeclaringType().getName())) {
      return false;
    }
    if (!m_member.name.equals(methodInfo.getName())) {
      return false;
    }
    ClassInfo[] parameterTypes = methodInfo.getParameterTypes();
    if (m_parameterTypeNames.length != parameterTypes.length) {//check on names length for lazyness optim
      return false;
    }
    for (int i = 0; i < m_parameterTypeNames.length; i++) {
      if (!m_parameterTypeNames[i].equals(parameterTypes[i].getName())) {
        return false;
      }
    }
    return true;
  }

  public int hashCode() {
    int result = 29;
    result = (29 * result) + m_declaringTypeName.hashCode();
    result = (29 * result) + m_member.name.hashCode();
    for (int i = 0; i < m_parameterTypeNames.length; i++) {
      result = (29 * result) + m_parameterTypeNames[i].hashCode();
    }
    return result;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(m_declaringTypeName);
    sb.append('.').append(m_member.name);
    sb.append(m_member.desc);
    return sb.toString();
  }

  /**
   * Update the parameter name given the parameter information
   * the index is the one from the register ie a long or double will needs 2 register
   *
   * @param registerIndex
   * @param parameterName
   */
  public void pushParameterNameFromRegister(int registerIndex, String parameterName) {
    int registerStart = 1;
    if (Modifier.isStatic(m_member.modifiers)) {
      registerStart = 0;
    }
    // assume we have a stack starting at the first parameter
    int registerIndexFrom0 = registerIndex - registerStart;
    Type[] parameters = Type.getArgumentTypes(m_member.desc);
    int typeIndex = AsmHelper.getTypeIndexOf(parameters, registerIndexFrom0);
    if (typeIndex >= 0 && typeIndex < m_parameterNames.length) {
      m_parameterNames[typeIndex] = parameterName;
    } else {
      throw new DefinitionException(
              "Could not register parameter named " + parameterName
                      + " from register " + registerIndex + " for " + m_member.name + "." + m_member.desc
      );
    }
  }
}