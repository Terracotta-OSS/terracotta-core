/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.definition;


import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.expression.ExpressionInfo;
import com.tc.aspectwerkz.aspect.DefaultMixinFactory;
import com.tc.aspectwerkz.intercept.Advisable;
import com.tc.aspectwerkz.intercept.AdvisableImpl;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.reflect.ClassInfoHelper;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.transform.TransformationConstants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.lang.ref.WeakReference;

/**
 * Definition for the mixin construct.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class MixinDefinition {

  private final static String DEFAULT_MIXIN_FACTORY = DefaultMixinFactory.class.getName().replace('/', '.');
  /**
   * The deployment model for the mixin.
   */
  private DeploymentModel m_deploymentModel;

  /**
   * Flags the mixin as transient.
   */
  private boolean m_isTransient;

  /**
   * The introduced methods info list.
   */
  private final List m_methodsToIntroduce = new ArrayList();

  /**
   * The interface classes name.
   */
  private final List m_interfaceClassNames = new ArrayList();

  /**
   * The class name for the mixin impl.
   */
  private final String m_mixinImplClassName;

  /**
   * The class loader.
   */
  private final WeakReference m_loaderRef;

  /**
   * The mixin expressions.
   */
  private ExpressionInfo[] m_expressionInfos = new ExpressionInfo[]{};

  /**
   * The attribute for the mixin.
   */
  private String m_attribute = "";

  /**
   * The factory class name.
   */
  private String m_factoryClassName;

  /**
   * The system definition.
   */
  private SystemDefinition m_systemDefinition;

  /**
   * The parameters passed to the mixin at definition time.
   */
  private Map m_parameters = new HashMap();

  /**
   * Construct a new definition for mixin.
   *
   * @param mixinClass      the mixin class
   * @param deploymentModel mixin deployment model
   * @param isTransient     transient flag
   * @param systemDef       the system definition
   */
  public MixinDefinition(ClassInfo mixinClass,
                         final DeploymentModel deploymentModel,
                         final boolean isTransient,
                         final SystemDefinition systemDef) {
    if (isSystemMixin(mixinClass)) {
      mixinClass = defineSystemMixin(mixinClass.getClassLoader());
    } else {
      List allInterfaces = ClassInfoHelper.collectInterfaces(mixinClass);
      for (Iterator iterator = allInterfaces.iterator(); iterator.hasNext();) {
        ClassInfo interfaceInfo = (ClassInfo) iterator.next();
        m_interfaceClassNames.add(interfaceInfo.getName());
      }

      List interfaceDeclaredMethods = ClassInfoHelper.collectMethodsFromInterfacesImplementedBy(mixinClass);
      List sortedMethodList = ClassInfoHelper.createInterfaceDefinedMethodList(
              mixinClass, interfaceDeclaredMethods
      );
      for (Iterator iterator = sortedMethodList.iterator(); iterator.hasNext();) {
        MethodInfo methodInfo = (MethodInfo) iterator.next();
        m_methodsToIntroduce.add(methodInfo);
      }
    }

    m_mixinImplClassName = mixinClass.getName();
    m_loaderRef = new WeakReference(mixinClass.getClassLoader());
    m_systemDefinition = systemDef;
    m_expressionInfos = new ExpressionInfo[]{};

    m_deploymentModel = deploymentModel;
    m_isTransient = isTransient;

    // default factory
    setFactoryClassName(DEFAULT_MIXIN_FACTORY);
  }

  /**
   * Sets the factory class name.
   *
   * @param factoryClassName
   */
  public void setFactoryClassName(final String factoryClassName) {
    m_factoryClassName = factoryClassName;
  }

  /**
   * Returns the factory class name.
   *
   * @return
   */
  public String getFactoryClassName() {
    return m_factoryClassName;
  }

  /**
   * Returns the methods to introduce.
   *
   * @return the methods to introduce
   */
  public List getMethodsToIntroduce() {
    return m_methodsToIntroduce;
  }

  /**
   * Returns the deployment model.
   *
   * @return the deployment model
   */
  public DeploymentModel getDeploymentModel() {
    return m_deploymentModel;
  }

  /**
   * Sets the deployment model.
   *
   * @param deploymentModel
   */
  public void setDeploymentModel(final DeploymentModel deploymentModel) {
    m_deploymentModel = deploymentModel;
  }

  /**
   * Checks if the mixin is transient.
   *
   * @return
   */
  public boolean isTransient() {
    return m_isTransient;
  }

  /**
   * Sets the mixin as transient.
   *
   * @param isTransient
   */
  public void setTransient(boolean isTransient) {
    m_isTransient = isTransient;
  }

  /**
   * Returns the class info for the mixin impl.
   *
   * @return the class info
   */
  public ClassInfo getMixinImpl() {
    return AsmClassInfo.getClassInfo(m_mixinImplClassName, (ClassLoader) m_loaderRef.get());
  }

  /**
   * Returns the expressions.
   *
   * @return the expressions array
   */
  public ExpressionInfo[] getExpressionInfos() {
    return m_expressionInfos;
  }

  /**
   * Returns the class name of the interface.
   *
   * @return the class name of the interface
   */
  public List getInterfaceClassNames() {
    return m_interfaceClassNames;
  }

  /**
   * Returns the attribute.
   *
   * @return the attribute
   */
  public String getAttribute() {
    return m_attribute;
  }

  /**
   * Sets the attribute.
   *
   * @param attribute the attribute
   */
  public void setAttribute(final String attribute) {
    m_attribute = attribute;
  }

  /**
   * Returns the system definition.
   *
   * @return the system definition
   */
  public SystemDefinition getSystemDefinition() {
    return m_systemDefinition;
  }

  /**
   * Adds a new expression info.
   *
   * @param expression a new expression info
   */
  public void addExpressionInfo(final ExpressionInfo expression) {
    final ExpressionInfo[] tmpExpressions = new ExpressionInfo[m_expressionInfos.length + 1];
    java.lang.System.arraycopy(m_expressionInfos, 0, tmpExpressions, 0, m_expressionInfos.length);
    tmpExpressions[m_expressionInfos.length] = expression;
    m_expressionInfos = new ExpressionInfo[m_expressionInfos.length + 1];
    java.lang.System.arraycopy(tmpExpressions, 0, m_expressionInfos, 0, tmpExpressions.length);
  }

  /**
   * Adds an array with new expression infos.
   *
   * @param expressions an array with new expression infos
   */
  public void addExpressionInfos(final ExpressionInfo[] expressions) {
    final ExpressionInfo[] tmpExpressions = new ExpressionInfo[m_expressionInfos.length + expressions.length];
    java.lang.System.arraycopy(m_expressionInfos, 0, tmpExpressions, 0, m_expressionInfos.length);
    java.lang.System.arraycopy(expressions, 0, tmpExpressions, m_expressionInfos.length, expressions.length);
    m_expressionInfos = new ExpressionInfo[m_expressionInfos.length + expressions.length];
    java.lang.System.arraycopy(tmpExpressions, 0, m_expressionInfos, 0, tmpExpressions.length);
  }

  /**
   * Defines system mixins.
   *
   * @param loader
   * @return
   */
  private ClassInfo defineSystemMixin(final ClassLoader loader) {
    // if advisable impl mixin getDefault the class info from the AsmClassInfo to keep the methods starting with aw$
    ClassInfo mixinClass = AsmClassInfo.getClassInfo(AdvisableImpl.class.getName(), loader);
    MethodInfo[] methods = mixinClass.getMethods();
    for (int i = 0; i < methods.length; i++) {
      MethodInfo method = methods[i];
      if (method.getName().startsWith(TransformationConstants.SYNTHETIC_MEMBER_PREFIX)
              || method.getName().startsWith("aw_")) {//aw$ not reachable in IDEs without AW source code
        m_methodsToIntroduce.add(method);
      }
    }
    m_interfaceClassNames.add(Advisable.class.getName());
    return mixinClass;
  }

  /**
   * Checks if the mixin is a system mixin.
   *
   * @param mixinClass
   * @return
   */
  private boolean isSystemMixin(final ClassInfo mixinClass) {
    return mixinClass.getName().equals(AdvisableImpl.class.getName());
  }

  /**
   * Adds a new parameter to the mixin.
   *
   * @param name  the name of the parameter
   * @param value the value for the parameter
   */
  public void addParameter(final String name, final String value) {
    m_parameters.put(name, value);
  }

  /**
   * Returns the parameters as a Map.
   *
   * @return the parameters
   */
  public Map getParameters() {
    return m_parameters;
  }

}
