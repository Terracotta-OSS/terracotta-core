/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.aspect;

import com.tc.asm.Type;
import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.definition.AdviceDefinition;
import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.ExpressionInfo;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;

import java.util.Arrays;

/**
 * Contains advice info, like indexes describing the aspect and a method (advice or introduced),
 * aspect manager etc.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 */
public class AdviceInfo /* implements Serializable */ {

  public final static AdviceInfo[] EMPTY_ADVICE_INFO_ARRAY = new AdviceInfo[0];

  // -- some magic index used in the m_methodToArgIndexes[] so that we know what to bind except advised target args
  // -- those constants MUST be negative since positive values are used for args(..) binding
  public final static int JOINPOINT_ARG = -0x1;
  public final static int STATIC_JOINPOINT_ARG = -0x2;
  public final static int TARGET_ARG = -0x3;
  public final static int THIS_ARG = -0x4;
  public final static int VALID_NON_AW_AROUND_CLOSURE_TYPE = -0x5;
  public final static int SPECIAL_ARGUMENT = -0x6;
  public static final int CUSTOM_JOIN_POINT_ARG = -0x7;

  /**
   * The method name.
   */
  private String m_methodName;

  /**
   * The method sig.
   */
  private String m_methodSignature;

  /**
   * The method's parameter types.
   */
  private Type[] m_methodParameterTypes;

  /**
   * The advice name
   * <adviceMethodName>[(... call signature)]
   */
  private final String m_name;

  /**
   * The aspect class name where this advice is defined.
   */
  private String m_aspectClassName;

  /**
   * The aspect qualified name - <uuid>/<aspectNickName or FQNclassName>
   */
  private String m_aspectQualifiedName;

  /**
   * The aspect deployment model
   */
  private DeploymentModel m_aspectDeploymentModel;

  /**
   * The advice method arg index mapped to the advisED target arg index.
   * If the value is greater or equal to 0, it is an args binding. Else, it is a magic index
   * (see constants JOINPOINT_ARG, STATIC_JOINPOINT_ARG, THIS_ARG, TARGET_ARG)
   */
  private int[] m_methodToArgIndexes;

  /**
   * The "special" argument type desc for the advice.
   */
  private String m_specialArgumentTypeDesc;

  /**
   * The "special" argument type name for the advice.
   */
  private String m_specialArgumentTypeName;

  /**
   * The advice type.
   */
  private AdviceType m_type;

  /**
   * Runtime check flag.
   */
  private boolean m_targetWithRuntimeCheck;

  /**
   * The expression info.
   */
  private ExpressionInfo m_expressionInfo;

  /**
   * The expression context.
   */
  private ExpressionContext m_expressionContext;

  /**
   * The advice definition for this advice.
   */
  private AdviceDefinition m_adviceDef;

  /**
   * TODO refactor - many member fields holds data that is in either the adviceDef (which is in the class) or the aspectDef (which is accessible from the adviceDef)
   * <p/>
   * Creates a new advice info.
   *
   * @param aspectQualifiedName
   * @param aspectClassName
   * @param aspectDeploymentModel
   * @param methodName
   * @param methodSignature
   * @param methodParameterTypes
   * @param type                   the advice type
   * @param specialArgumentType    the special arg type
   * @param adviceName             full qualified advice method name (aspectFQN/advice(call sig))
   * @param targetWithRuntimeCheck true if a runtime check is needed based on target instance
   * @param expressionInfo
   * @param expressionContext
   * @param adviceDef
   */
  public AdviceInfo(final String aspectQualifiedName,
                    final String aspectClassName,
                    final DeploymentModel aspectDeploymentModel,
                    final String methodName,
                    final String methodSignature,
                    final Type[] methodParameterTypes,
                    final AdviceType type,
                    final String specialArgumentType,
                    final String adviceName,
                    final boolean targetWithRuntimeCheck,
                    final ExpressionInfo expressionInfo,
                    final ExpressionContext expressionContext,
                    final AdviceDefinition adviceDef) {
    m_aspectQualifiedName = aspectQualifiedName;
    m_aspectClassName = aspectClassName;
    m_aspectDeploymentModel = aspectDeploymentModel;
    m_methodName = methodName;
    m_methodSignature = methodSignature;
    m_methodParameterTypes = methodParameterTypes;
    m_type = type;
    if (specialArgumentType != null && specialArgumentType.length() > 0) {//AW-434
      m_specialArgumentTypeDesc = AsmHelper.convertReflectDescToTypeDesc(specialArgumentType);
      m_specialArgumentTypeName = specialArgumentType.replace('.', '/');
    }
    m_name = adviceName;
    m_targetWithRuntimeCheck = targetWithRuntimeCheck;
    m_expressionInfo = expressionInfo;
    m_expressionContext = expressionContext;
    m_adviceDef = adviceDef;
  }

  /**
   * Return the method name.
   *
   * @return the method name
   */
  public String getMethodName() {
    return m_methodName;
  }

  /**
   * Return the method signature.
   *
   * @return the method signature
   */
  public String getMethodSignature() {
    return m_methodSignature;
  }

  /**
   * Return the method name.
   *
   * @return the method name
   */
  public Type[] getMethodParameterTypes() {
    return m_methodParameterTypes;
  }

  /**
   * Returns the aspect qualified name.
   *
   * @return the aspect qualified name
   */
  public String getAspectQualifiedName() {
    return m_aspectQualifiedName;
  }

  /**
   * Returns the aspect FQN className.
   *
   * @return the aspect class name
   */
  public String getAspectClassName() {
    return m_aspectClassName;
  }

  /**
   * Returns the aspect deployment model
   *
   * @return
   */
  public DeploymentModel getAspectDeploymentModel() {
    return m_aspectDeploymentModel;
  }

  /**
   * Returns the name of the advice.
   *
   * @return
   */
  public String getName() {
    return m_name;
  }

  /**
   * Sets the advice method to target method arg mapping A value of -1 means "not mapped"
   *
   * @param map
   */
  public void setMethodToArgIndexes(final int[] map) {
    m_methodToArgIndexes = map;
  }

  /**
   * Returns the advice method to target method arg index mapping.
   *
   * @return the indexes
   */
  public int[] getMethodToArgIndexes() {
    return m_methodToArgIndexes;
  }

  /**
   * Returns the special argument type desc.
   *
   * @return
   */
  public String getSpecialArgumentTypeDesc() {
    return m_specialArgumentTypeDesc;
  }

  /**
   * Returns the special argument type name.
   *
   * @return
   */
  public String getSpecialArgumentTypeName() {
    return m_specialArgumentTypeName;
  }

  /**
   * Returns the advice type.
   *
   * @return
   */
  public AdviceType getType() {
    return m_type;
  }

  /**
   * Checks if the target has a runtime check.
   *
   * @return
   */
  public boolean hasTargetWithRuntimeCheck() {
    return m_targetWithRuntimeCheck;
  }

  /**
   * Returns the expression info.
   *
   * @return
   */
  public ExpressionInfo getExpressionInfo() {
    return m_expressionInfo;
  }

  /**
   * Returns the expression context.
   *
   * @return
   */
  public ExpressionContext getExpressionContext() {
    return m_expressionContext;
  }

  /**
   * Returns the advice definition.
   *
   * @return
   */
  public AdviceDefinition getAdviceDefinition() {
    return m_adviceDef;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer("AdviceInfo[");
    sb.append(m_type).append(',');
    sb.append(m_aspectQualifiedName).append(',');
    sb.append(m_name).append(',');
    sb.append(m_methodName).append(',');
    sb.append(m_methodSignature).append(',');
    sb.append(Arrays.toString(m_methodParameterTypes)).append(',');
    sb.append(m_specialArgumentTypeDesc).append(',');
    sb.append(m_expressionInfo).append(',');
    sb.append(m_expressionContext).append(',');
    sb.append(m_targetWithRuntimeCheck).append(']');
    sb.append(hashCode());
    return sb.toString();
  }

}
