/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.expression;

import com.tc.aspectwerkz.reflect.ReflectionInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.ConstructorInfo;
import com.tc.aspectwerkz.reflect.StaticInitializationInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;

import java.util.HashMap;

/**
 * The expression context for AST evaluation.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class ExpressionContext {
  public static final int INFO_NOT_AVAILABLE = -1;

  public static final int METHOD_INFO = 0;

  public static final int CONSTRUCTOR_INFO = 1;

  public static final int FIELD_INFO = 2;

  public static final int CLASS_INFO = 3;

  private static final int STATIC_INFO = 4;

  private final int m_reflectionInfoType;

  private final PointcutType m_pointcutType;

  private final ReflectionInfo m_matchingReflectionInfo;

  private final ReflectionInfo m_withinReflectionInfo;

  private boolean m_inCflowSubAST = false;

  private boolean m_cflowEvaluation = false;

  private boolean m_hasBeenVisitingCflow = false;

  private int m_currentTargetArgsIndex = 0;

  /**
   * Expression to advised target (method / ctor) argument index map.
   * It depends on the matching context and the pointcut signature, as well as args(..)
   *
   * Value of this field is cloned, so had to use HashMap type.
   */
  public HashMap m_exprIndexToTargetIndex = new HashMap();

  /**
   * The variable name corresponding to the this(..) designator,
   * or null if nothing is bound (this(<type>) or no this(..))
   */
  public String m_thisBoundedName = null;

  /**
   * The variable name corresponding to the target(..) designator,
   * or null if nothing is bound (target(<type>) or no target(..))
   */
  public String m_targetBoundedName = null;

  /**
   * Set to true when we encounter a poincut using target(..) and when match cannot be done without a
   * runtime check with instance of.
   */
  public boolean m_targetWithRuntimeCheck = false;

  /**
   * Creates a new expression context.
   *
   * @param pointcutType
   * @param reflectionInfo       - can be null f.e. with early evaluation of CALL pointcut
   * @param withinReflectionInfo
   */
  public ExpressionContext(final PointcutType pointcutType,
                           final ReflectionInfo reflectionInfo,
                           final ReflectionInfo withinReflectionInfo) {
    if (pointcutType == null) {
      throw new IllegalArgumentException("pointcut type can not be null");
    }
    m_pointcutType = pointcutType;
    m_matchingReflectionInfo = reflectionInfo;
    if (withinReflectionInfo != null) {
      m_withinReflectionInfo = withinReflectionInfo;
    } else {
      if (PointcutType.EXECUTION.equals(pointcutType)
              || PointcutType.STATIC_INITIALIZATION.equals(pointcutType)
              || PointcutType.WITHIN.equals(pointcutType)) {
        m_withinReflectionInfo = m_matchingReflectionInfo;
      } else {
        m_withinReflectionInfo = null;
      }
    }
    if (reflectionInfo instanceof MethodInfo) {
      m_reflectionInfoType = METHOD_INFO;
    } else if (reflectionInfo instanceof ConstructorInfo) {
      m_reflectionInfoType = CONSTRUCTOR_INFO;
    } else if (reflectionInfo instanceof FieldInfo) {
      m_reflectionInfoType = FIELD_INFO;
    } else if (reflectionInfo instanceof ClassInfo) {
      m_reflectionInfoType = CLASS_INFO;
    } else if (reflectionInfo instanceof StaticInitializationInfo) {
      m_reflectionInfoType = STATIC_INFO;
    } else {
      m_reflectionInfoType = INFO_NOT_AVAILABLE;// used for early eval on CALL
    }
  }

  public ReflectionInfo getReflectionInfo() {
    return m_matchingReflectionInfo;
  }

  public ReflectionInfo getWithinReflectionInfo() {
    return m_withinReflectionInfo;
  }

  public boolean hasExecutionPointcut() {
    return m_pointcutType.equals(PointcutType.EXECUTION);
  }

  public boolean hasCallPointcut() {
    return m_pointcutType.equals(PointcutType.CALL);
  }

  public boolean hasSetPointcut() {
    return m_pointcutType.equals(PointcutType.SET);
  }

  public boolean hasGetPointcut() {
    return m_pointcutType.equals(PointcutType.GET);
  }

  public boolean hasHandlerPointcut() {
    return m_pointcutType.equals(PointcutType.HANDLER);
  }

  public boolean hasStaticInitializationPointcut() {
    return m_pointcutType.equals(PointcutType.STATIC_INITIALIZATION);
  }

  public boolean hasWithinPointcut() {
    return m_pointcutType.equals(PointcutType.WITHIN);
  }
//
//    public boolean hasHasMethodPointcut() {
//        return m_pointcutType.equals(PointcutType.HAS_METHOD);
//    }
//
//    public boolean hasHasFieldPointcut() {
//        return m_pointcutType.equals(PointcutType.HAS_FIELD);
//    }

  public boolean hasWithinReflectionInfo() {
    return m_withinReflectionInfo != null;
  }

  public boolean hasMethodInfo() {
    return m_reflectionInfoType == METHOD_INFO;
  }

  public boolean hasConstructorInfo() {
    return m_reflectionInfoType == CONSTRUCTOR_INFO;
  }

  public boolean hasFieldInfo() {
    return m_reflectionInfoType == FIELD_INFO;
  }

  public boolean hasClassInfo() {
    return m_reflectionInfoType == CLASS_INFO;
  }

  public boolean hasReflectionInfo() {
    return m_reflectionInfoType != INFO_NOT_AVAILABLE;
  }

  public void setInCflowSubAST(final boolean inCflowAST) {
    m_inCflowSubAST = inCflowAST;
  }

  public boolean inCflowSubAST() {
    return m_inCflowSubAST;
  }

  public void setHasBeenVisitingCflow(final boolean hasBeenVisitingCflow) {
    m_hasBeenVisitingCflow = hasBeenVisitingCflow;
  }

  public boolean hasBeenVisitingCflow() {
    return m_hasBeenVisitingCflow;
  }

  public boolean getCflowEvaluation() {
    return m_cflowEvaluation;
  }

  public void setCflowEvaluation(boolean cflowEvaluation) {
    m_cflowEvaluation = cflowEvaluation;
  }

  public int getCurrentTargetArgsIndex() {
    return m_currentTargetArgsIndex;
  }

  public void setCurrentTargetArgsIndex(int argsIndex) {
    this.m_currentTargetArgsIndex = argsIndex;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ExpressionContext)) {
      return false;
    }
    final ExpressionContext expressionContext = (ExpressionContext) o;
    if (m_reflectionInfoType != expressionContext.m_reflectionInfoType) {
      return false;
    }
    if (!m_matchingReflectionInfo.equals(expressionContext.m_matchingReflectionInfo)) {
      return false;
    }
    if (!m_pointcutType.equals(expressionContext.m_pointcutType)) {
      return false;
    }
    if ((m_withinReflectionInfo != null) ?
            (!m_withinReflectionInfo
                    .equals(expressionContext.m_withinReflectionInfo)) :
            (expressionContext.m_withinReflectionInfo != null)) {
      return false;
    }
    return true;
  }

  public int hashCode() {
    int result;
    result = m_pointcutType.hashCode();
    result = (29 * result) + m_matchingReflectionInfo.hashCode();
    result = (29 * result) + ((m_withinReflectionInfo != null) ? m_withinReflectionInfo.hashCode() : 0);
    result = (29 * result) + m_reflectionInfoType;
    return result;
  }

  public PointcutType getPointcutType() {
    return m_pointcutType;
  }

  public void resetRuntimeState() {
    m_targetBoundedName = null;
    m_thisBoundedName = null;
    m_exprIndexToTargetIndex = new HashMap();
    m_targetWithRuntimeCheck = false;
  }

  public String getDebugString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[ExpressionContext:");
		buffer.append(" INFO_NOT_AVAILABLE: ");
		buffer.append(INFO_NOT_AVAILABLE);
		buffer.append(" METHOD_INFO: ");
		buffer.append(METHOD_INFO);
		buffer.append(" CONSTRUCTOR_INFO: ");
		buffer.append(CONSTRUCTOR_INFO);
		buffer.append(" FIELD_INFO: ");
		buffer.append(FIELD_INFO);
		buffer.append(" CLASS_INFO: ");
		buffer.append(CLASS_INFO);
		buffer.append(" STATIC_INFO: ");
		buffer.append(STATIC_INFO);
		buffer.append(" m_reflectionInfoType: ");
		buffer.append(m_reflectionInfoType);
		buffer.append(" m_pointcutType: ");
		buffer.append(m_pointcutType);
		buffer.append(" m_matchingReflectionInfo: ");
		buffer.append(m_matchingReflectionInfo);
		buffer.append(" m_withinReflectionInfo: ");
		buffer.append(m_withinReflectionInfo);
		buffer.append(" m_inCflowSubAST: ");
		buffer.append(m_inCflowSubAST);
		buffer.append(" m_cflowEvaluation: ");
		buffer.append(m_cflowEvaluation);
		buffer.append(" m_hasBeenVisitingCflow: ");
		buffer.append(m_hasBeenVisitingCflow);
		buffer.append(" m_currentTargetArgsIndex: ");
		buffer.append(m_currentTargetArgsIndex);
		buffer.append(" m_exprIndexToTargetIndex: ");
		buffer.append(m_exprIndexToTargetIndex);
		buffer.append(" m_thisBoundedName: ");
		buffer.append(m_thisBoundedName);
		buffer.append(" m_targetBoundedName: ");
		buffer.append(m_targetBoundedName);
		buffer.append(" m_targetWithRuntimeCheck: ");
		buffer.append(m_targetWithRuntimeCheck);
		buffer.append("]");
		return buffer.toString();
	}


}