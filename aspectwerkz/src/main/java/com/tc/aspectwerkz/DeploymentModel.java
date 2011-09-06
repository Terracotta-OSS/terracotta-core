/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz;

import com.tc.aspectwerkz.perx.PerObjectAspect;

/**
 * Enum containing the different deployment model types.
 * <p/>
 * Note: equals does not check for pointcut equality for perthis/pertarget but
 * does only checks for types
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class DeploymentModel {

  public static final DeploymentModel PER_JVM = new DeploymentModel("perJVM");
  public static final DeploymentModel PER_CLASS = new DeploymentModel("perClass");
  public static final DeploymentModel PER_INSTANCE = new DeploymentModel("perInstance");

  public static final DeploymentModel PER_TARGET = new DeploymentModel("perTarget");
  public static final DeploymentModel PER_THIS = new DeploymentModel("perThis");
  public static final DeploymentModel PER_CFLOW = new DeploymentModel("perCflow");
  public static final DeploymentModel PER_CFLOWBELOW = new DeploymentModel("perCflowbelow");

  private static final String THIS_POINTCUT = "this(" + PerObjectAspect.ADVICE_ARGUMENT_NAME + ")";
  private static final String TARGET_POINTCUT = "target(" + PerObjectAspect.ADVICE_ARGUMENT_NAME + ")";

  protected final String m_name;

  DeploymentModel(String name) {
    m_name = name;
  }

  public String toString() {
    return m_name;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DeploymentModel)) {
      return false;
    }
    final DeploymentModel adviceType = (DeploymentModel) o;
    if ((m_name != null) ? (!m_name.equals(adviceType.m_name)) : (adviceType.m_name != null)) {
      return false;
    }
    return true;
  }

  public int hashCode() {
    return ((m_name != null) ? m_name.hashCode() : 0);
  }

  public static DeploymentModel getDeploymentModelFor(final String deploymentModelAsString) {
    if (deploymentModelAsString == null || deploymentModelAsString.equals("")) {
      return PER_JVM; // default is PER_JVM
    }
    if (deploymentModelAsString.equalsIgnoreCase(PER_JVM.toString())) {
      return PER_JVM;
    } else if (deploymentModelAsString.equalsIgnoreCase(PER_CLASS.toString())) {
      return PER_CLASS;
    } else if (deploymentModelAsString.equalsIgnoreCase(PER_INSTANCE.toString())) {
      return PER_INSTANCE;
    } else if (deploymentModelAsString.equalsIgnoreCase(PER_CFLOW.toString())) {
      return PER_CFLOW;
    } else if (deploymentModelAsString.equalsIgnoreCase(PER_CFLOWBELOW.toString())) {
      return PER_CFLOWBELOW;
    } else if (deploymentModelAsString.equalsIgnoreCase(PER_THIS.toString())) {
      return PER_THIS;
    } else if (deploymentModelAsString.equalsIgnoreCase(PER_TARGET.toString())) {
      return PER_TARGET;
      // below support for more advanced schemes.
    } else if (deploymentModelAsString.toLowerCase().startsWith(PER_THIS.m_name.toLowerCase())) {
      return new PointcutControlledDeploymentModel(PER_THIS.m_name,
              getDeploymentExpression(deploymentModelAsString, THIS_POINTCUT)
      );
    } else if (deploymentModelAsString.toLowerCase().startsWith(PER_TARGET.m_name.toLowerCase())) {
      return new PointcutControlledDeploymentModel(PER_TARGET.m_name,
              getDeploymentExpression(deploymentModelAsString, TARGET_POINTCUT)
      );
    } else {
      System.out.println(
              "AW::WARNING - no such deployment model [" + deploymentModelAsString + "] using default (perJVM)"
      );
      return PER_JVM; // falling back to default - PER_JVM
    }
  }

  /**
   * @param deploymentModelAsString
   * @return
   */
  private static String getDeploymentExpression(String deploymentModelAsString,
                                                final String pointcut) {
    int startIndex = deploymentModelAsString.indexOf('(');
    int endIndex = deploymentModelAsString.lastIndexOf(')');

    if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
      System.out.println(
              "AW::ERROR - wrong deployment model definition [" + deploymentModelAsString + "]");

      return "";
    }

    return deploymentModelAsString.substring(startIndex + 1, endIndex).trim()
            + " && "
            + pointcut;
  }

  /**
   * perthis.. pertarget.. deployment model depends on a pointcut expression
   */
  public static final class PointcutControlledDeploymentModel extends DeploymentModel {

    private String m_expression;

    PointcutControlledDeploymentModel(String name, String expression) {
      super(name);
      m_expression = expression;
    }

    public String getDeploymentExpression() {
      return m_expression;
    }

    public String toString() {
      // returns only the name, not the expression
      return m_name;
    }

    public boolean equals(Object o) {
      return super.equals(o);
    }

    public int hashCode() {
      return super.hashCode();
    }
  }
}