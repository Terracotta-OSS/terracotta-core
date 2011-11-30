/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect;

/**
 * Holds a tuple that consists of the class info and the info for a specific method.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 */
public class CflowMetaData {
  /**
   * The class name.
   */
  private final String m_className;

  /**
   * The class info.
   */
  private ClassInfo m_classMetaData;

  /**
   * The method info.
   */
  private final MethodInfo m_methodMetaData;

  /**
   * Creates a new ClassNameMethodInfoTuple.
   *
   * @param classMetaData  the class metaData
   * @param methodMetaData the method info
   */
  public CflowMetaData(final ClassInfo classMetaData, final MethodInfo methodMetaData) {
    m_className = classMetaData.getName();
    m_classMetaData = classMetaData;
    m_methodMetaData = methodMetaData;
  }

  /**
   * Returns the class info.
   *
   * @return the class info
   */
  public ClassInfo getClassInfo() {
    return m_classMetaData;
  }

  /**
   * Returns the class name.
   *
   * @return the class name
   */
  public String getClassName() {
    return m_className;
  }

  /**
   * Returns the method info.
   *
   * @return the method info
   */
  public MethodInfo getMethodInfo() {
    return m_methodMetaData;
  }

  // --- over-ridden methods ---
  public String toString() {
    return '[' + super.toString() + ": " + ',' + m_className + ',' + m_classMetaData + ',' + m_methodMetaData +
            ']';
  }

  public int hashCode() {
    int result = 17;
    result = (37 * result) + hashCodeOrZeroIfNull(m_className);
    result = (37 * result) + hashCodeOrZeroIfNull(m_classMetaData);
    result = (37 * result) + hashCodeOrZeroIfNull(m_methodMetaData);
    return result;
  }

  protected static int hashCodeOrZeroIfNull(final Object o) {
    if (null == o) {
      return 19;
    }
    return o.hashCode();
  }

  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CflowMetaData)) {
      return false;
    }
    final CflowMetaData obj = (CflowMetaData) o;
    return areEqualsOrBothNull(obj.m_className, this.m_className)
            && areEqualsOrBothNull(obj.m_classMetaData, this.m_classMetaData)
            && areEqualsOrBothNull(obj.m_methodMetaData, this.m_methodMetaData);
  }

  protected static boolean areEqualsOrBothNull(final Object o1, final Object o2) {
    if (null == o1) {
      return (null == o2);
    }
    return o1.equals(o2);
  }
}