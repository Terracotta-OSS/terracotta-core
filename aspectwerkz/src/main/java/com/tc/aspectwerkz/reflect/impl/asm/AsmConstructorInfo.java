/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect.impl.asm;

import com.tc.backport175.bytecode.AnnotationElement;
import com.tc.asm.Type;

import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.ConstructorInfo;

/**
 * ASM implementation of the ConstructorInfo interface.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class AsmConstructorInfo extends AsmMemberInfo implements ConstructorInfo {

    /**
     * A list with the parameter type names.
     */
    private final String[] m_parameterTypeNames;

    /**
     * A list with the exception type names.
     */
    private final String[] m_exceptionTypeNames;

    /**
     * A list with the parameter types.
     */
    private ClassInfo[] m_parameterTypes = null;

    /**
     * A list with the exception types.
     */
    private ClassInfo[] m_exceptionTypes = null;

    /**
     * Creates a new method meta data instance.
     *
     * @param method
     * @param declaringType
     * @param loader
     */
    AsmConstructorInfo(final MethodStruct method, final String declaringType, final ClassLoader loader) {
        super(method, declaringType, loader);
        Type[] argTypes = Type.getArgumentTypes(method.desc);
        m_parameterTypeNames = new String[argTypes.length];
        for (int i = 0; i < argTypes.length; i++) {
            m_parameterTypeNames[i] = argTypes[i].getClassName();
        }
        // TODO how to do exceptions?
        m_exceptionTypeNames = new String[]{};
    }

    /**
     * Returns the signature for the element.
     *
     * @return the signature for the element
     */
    public String getSignature() {
        return AsmHelper.getConstructorDescriptor(this);
    }

    public String getGenericsSignature() {
      return m_member.signature;
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
        return getDeclaringType().getAnnotationReader().getConstructorAnnotationElements(m_member.desc);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConstructorInfo)) {
            return false;
        }
        ConstructorInfo constructorInfo = (ConstructorInfo) o;
        if (!m_declaringTypeName.equals(constructorInfo.getDeclaringType().getName())) {
            return false;
        }
        if (!m_member.name.equals(constructorInfo.getName())) {
            return false;
        }
        ClassInfo[] parameterTypes = constructorInfo.getParameterTypes();
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
}
