/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.perx;

import com.tc.aspectwerkz.AspectContext;
import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.expression.ExpressionInfo;
import com.tc.aspectwerkz.aspect.AdviceType;
import com.tc.aspectwerkz.aspect.management.HasInstanceLevelAspect;
import com.tc.aspectwerkz.definition.AdviceDefinition;
import com.tc.aspectwerkz.definition.AspectDefinition;
import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.transform.TransformationConstants;

/**
 * Generic aspect used by perX deployment modes to initialize the aspect instance.
 * It gets registered programatically when finding perX aspects.
 * fake
 *
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class PerObjectAspect {

  /**
   * PerObjectAspect class name
   */
  public static final String PEROBJECT_ASPECT_NAME = PerObjectAspect.class.getName().replace('/', '.');

  /**
   * Name of system advice that will bind the aspect to the perX X instance.
   * This advice is bound to the "X && target(aw_instance)" pointcut for pertarget(X)
   * and "X && this(aw_instance)" pointcut for perthis(X) and thus its signature
   * is a beforePerObjecr(HasInstanceLevelAspect aw_instance).
   */
  private static final String BEFORE_ADVICE_NAME = "beforePerObject";

  /**
   * Name of the advice argument where we bound the perX X instance.
   */
  public static final String ADVICE_ARGUMENT_NAME = "aw_Instance";

  /**
   * ClassInfo and method Info for the PerObjectAspect
   */
  private static final ClassInfo PEROBJECT_CLASSINFO = JavaClassInfo.getClassInfo(PerObjectAspect.class);
  private static MethodInfo BEFORE_ADVICE_METHOD_INFO;

  static {
    MethodInfo[] methods = PEROBJECT_CLASSINFO.getMethods();
    for (int i = 0; i < methods.length; i++) {
      if (PerObjectAspect.BEFORE_ADVICE_NAME.equals(methods[i].getName())) {
        BEFORE_ADVICE_METHOD_INFO = methods[i];
        break;
      }
    }
    if (BEFORE_ADVICE_METHOD_INFO == null) {
      throw new Error("Could not find PerObjectAspect." + BEFORE_ADVICE_NAME);
    }
  }

  /**
   * One PerObjectAspect instance gets created for each X of perthis(X) / pertarget(X) and
   * is passed as aspect context parameters the Qname of the perX aspect for which it acts
   * and the container class name of that one.
   */
  private static final String ASPECT_QNAME_PARAM = "perobject.aspect.qname";
  private static final String CONTAINER_CLASSNAME_PARAM = "perobject.container.classname";

  private static final String ADVICE_ARGUMENT_TYPE = TransformationConstants.HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME.replace('/', '.');

  private static final String ADVICE_SIGNATURE = BEFORE_ADVICE_NAME
          + "("
          + ADVICE_ARGUMENT_TYPE
          + " "
          + ADVICE_ARGUMENT_NAME
          + ")";

  private final String m_aspectQName;

  private final String m_containerClassName;

  /**
   * PerObjectAspect constructor.
   * We keep track of the aspectQname and container class name to further call Aspects.aspectOf
   *
   * @param ctx
   */
  public PerObjectAspect(AspectContext ctx) {
    m_aspectQName = ctx.getParameter(ASPECT_QNAME_PARAM);
    m_containerClassName = ctx.getParameter(CONTAINER_CLASSNAME_PARAM);
  }

  /**
   * Before perPointcut && this/target(targetInstance) bound that will associate the aspect and the instance.
   * Note: do not refactor the names here without refactoring the constants.
   */
  public void beforePerObject(HasInstanceLevelAspect aw_instance) {
    if (aw_instance == null) {
      return;
    }
    aw_instance.aw$getAspect(getClass());
  }

  /**
   * Creates the generic AspectDefinition for the PerObjectAspect
   *
   * @param systemDefinition
   * @param aspectDefinition
   * @return definition for the perObjectAspect for the given perX aspect
   */
  public static AspectDefinition getAspectDefinition(SystemDefinition systemDefinition,
                                                     AspectDefinition aspectDefinition) {
    DeploymentModel.PointcutControlledDeploymentModel deploymentModel =
            (DeploymentModel.PointcutControlledDeploymentModel) aspectDefinition.getDeploymentModel();

    AspectDefinition perXSystemAspectDef = new AspectDefinition(
            getAspectName(aspectDefinition.getQualifiedName(), deploymentModel),
            PEROBJECT_CLASSINFO,
            systemDefinition);

    perXSystemAspectDef.setDeploymentModel(DeploymentModel.PER_JVM);
    perXSystemAspectDef.addParameter(PerObjectAspect.ASPECT_QNAME_PARAM, aspectDefinition.getQualifiedName());
    perXSystemAspectDef.addParameter(PerObjectAspect.CONTAINER_CLASSNAME_PARAM, aspectDefinition.getContainerClassName());

    ExpressionInfo expressionInfo = createExpressionInfo(deploymentModel,
            aspectDefinition.getQualifiedName(),
            PEROBJECT_CLASSINFO.getClassLoader()
    );

    perXSystemAspectDef.addBeforeAdviceDefinition(
            new AdviceDefinition(
                    PerObjectAspect.ADVICE_SIGNATURE,
                    AdviceType.BEFORE,
                    null,
                    perXSystemAspectDef.getName(),
                    PEROBJECT_ASPECT_NAME,
                    expressionInfo,
                    BEFORE_ADVICE_METHOD_INFO,
                    perXSystemAspectDef
            )
    );

    return perXSystemAspectDef;
  }

  /**
   * Naming strategy depends on the perX(X) X pointcut.
   * One definition is created per perX X pointcut hashcode, thus perthis(X) and pertarget(X)
   * are sharing the same system aspect perObjectAspect definition.
   * Note: depends on perX aspect Qname to support pointcut references of the same name in 2 different aspect.
   *
   * @param perXAspectQName Qname of the perX deployed aspect
   * @param deploymentModel
   * @return
   */
  private static String getAspectName(String perXAspectQName,
                                      DeploymentModel.PointcutControlledDeploymentModel deploymentModel) {
    return PEROBJECT_ASPECT_NAME + '_' + perXAspectQName.hashCode() + '_' + deploymentModel.hashCode();
  }

  /**
   * Create the expression info where to bind the perObjectAspect for the given perX model
   *
   * @param deployModel
   * @param qualifiedName of the perX Aspect
   * @param cl
   * @return
   */
  public static ExpressionInfo createExpressionInfo(final DeploymentModel.PointcutControlledDeploymentModel deployModel,
                                                    final String qualifiedName,
                                                    final ClassLoader cl) {
    ExpressionInfo expressionInfo = new ExpressionInfo(deployModel.getDeploymentExpression(), qualifiedName);
    expressionInfo.addArgument(PerObjectAspect.ADVICE_ARGUMENT_NAME,
            PerObjectAspect.ADVICE_ARGUMENT_TYPE,
            cl);

    return expressionInfo;
  }

}
