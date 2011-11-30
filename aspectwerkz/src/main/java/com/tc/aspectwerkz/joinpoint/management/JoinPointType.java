/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint.management;

/**
 * Enumeration for all join point types.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public final class JoinPointType {

  public static final int METHOD_EXECUTION_INT = 1;
  public static final int METHOD_CALL_INT = 2;
  public static final int CONSTRUCTOR_EXECUTION_INT = 3;
  public static final int CONSTRUCTOR_CALL_INT = 4;
  public static final int FIELD_SET_INT = 5;
  public static final int FIELD_GET_INT = 6;
  public static final int HANDLER_INT = 7;
  public static final int STATIC_INITIALIZATION_INT = 8;


  public static final JoinPointType METHOD_EXECUTION = new JoinPointType(METHOD_EXECUTION_INT);

  public static final JoinPointType METHOD_CALL = new JoinPointType(METHOD_CALL_INT);

  public static final JoinPointType CONSTRUCTOR_EXECUTION = new JoinPointType(CONSTRUCTOR_EXECUTION_INT);

  public static final JoinPointType CONSTRUCTOR_CALL = new JoinPointType(CONSTRUCTOR_CALL_INT);

  public static final JoinPointType FIELD_SET = new JoinPointType(FIELD_SET_INT);

  public static final JoinPointType FIELD_GET = new JoinPointType(FIELD_GET_INT);

  public static final JoinPointType HANDLER = new JoinPointType(HANDLER_INT);

  public static final JoinPointType STATIC_INITIALIZATION = new JoinPointType(STATIC_INITIALIZATION_INT);

  private int m_int;

  private JoinPointType(int asInt) {
    m_int = asInt;
  }

  public String toString() {
    switch (m_int) {
      case METHOD_EXECUTION_INT:
        return "MethodExecution";
      case METHOD_CALL_INT:
        return "MethodCall";
      case CONSTRUCTOR_EXECUTION_INT:
        return "ConstructorExecution";
      case CONSTRUCTOR_CALL_INT:
        return "ConstructorCall";
      case FIELD_GET_INT:
        return "FieldGet";
      case FIELD_SET_INT:
        return "FieldSet";
      case HANDLER_INT:
        return "Handler";
      case STATIC_INITIALIZATION_INT:
        return "StaticInitialization";
      default:
        throw new Error("not supported join point type");
    }
  }

  public static JoinPointType fromInt(int asInt) {
    return new JoinPointType(asInt);
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof JoinPointType)) return false;

    final JoinPointType joinPointType = (JoinPointType) o;

    if (m_int != joinPointType.m_int) return false;

    return true;
  }

  public int hashCode() {
    return m_int;
  }
}