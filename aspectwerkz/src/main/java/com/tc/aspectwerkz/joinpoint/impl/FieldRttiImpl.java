/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint.impl;

import com.tc.aspectwerkz.joinpoint.FieldRtti;
import com.tc.aspectwerkz.joinpoint.Rtti;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

/**
 * Implementation for the field signature.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonï¿½r </a>
 */
public class FieldRttiImpl implements FieldRtti {
  private final FieldSignatureImpl m_signature;

  private WeakReference m_thisRef;

  private WeakReference m_targetRef;

  private Object m_fieldValue;

  /**
   * Creates a new field RTTI.
   *
   * @param signature
   * @param thisInstance
   * @param targetInstance
   */
  public FieldRttiImpl(final FieldSignatureImpl signature, final Object thisInstance, final Object targetInstance) {
    m_signature = signature;
    m_thisRef = new WeakReference(thisInstance);
    m_targetRef = new WeakReference(targetInstance);
  }

  /**
   * Clones the RTTI instance.
   *
   * @param thisInstance
   * @param targetInstance
   * @return
   */
  public Rtti cloneFor(final Object thisInstance, final Object targetInstance) {
    return new FieldRttiImpl(m_signature, thisInstance, targetInstance);
  }

  /**
   * Returns the target instance.
   *
   * @return the target instance
   */
  public Object getTarget() {
    return m_targetRef.get();
  }

  /**
   * Returns the instance currently executing.
   *
   * @return the instance currently executing
   */
  public Object getThis() {
    return m_thisRef.get();
  }

  /**
   * Returns the declaring class.
   *
   * @return the declaring class
   */
  public Class getDeclaringType() {
    return m_signature.getDeclaringType();
  }

  /**
   * Returns the modifiers for the signature. <p/>Could be used like this:
   * <p/>
   * <pre>
   * boolean isPublic = java.lang.reflect.Modifier.isPublic(signature.getModifiers());
   * </pre>
   *
   * @return the mofifiers
   */
  public int getModifiers() {
    return m_signature.getModifiers();
  }

  /**
   * Returns the name (f.e. name of method of field).
   *
   * @return the name
   */
  public String getName() {
    return m_signature.getName();
  }

  /**
   * Returns the field.
   *
   * @return the field
   */
  public Field getField() {
    return m_signature.getField();
  }

  /**
   * Returns the field type.
   *
   * @return the field type
   */
  public Class getFieldType() {
    return m_signature.getFieldType();
  }

  /**
   * Returns the value of the field.
   *
   * @return the value of the field
   */
  public Object getFieldValue() {
    return m_fieldValue;
  }

  /**
   * Sets the value of the field.
   *
   * @param fieldValue the value of the field
   */
  public void setFieldValue(final Object fieldValue) {
    m_fieldValue = fieldValue;
  }

  /**
   * Returns a string representation of the signature.
   *
   * @return a string representation
   * @TODO: implement toString to something meaningful
   */
  public String toString() {
    return super.toString();
  }

  /**
   * TODO: Needed for JIT compiler. Remove for 2.0.
   *
   * @return
   */
  public Object[] getParameterValues() {
    return new Object[]{m_fieldValue};
  }
}