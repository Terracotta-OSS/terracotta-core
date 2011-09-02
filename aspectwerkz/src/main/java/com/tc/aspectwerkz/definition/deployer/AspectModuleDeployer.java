/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

package com.tc.aspectwerkz.definition.deployer;

import com.tc.aspectwerkz.DeploymentModel;

/**
 * TODO document class
 *
 * @author Jonas Bon&#233;r
 */
public interface AspectModuleDeployer {

  /**
   * Creates, registers and returns an aspect definition builder.
   * Use-case: Get an aspect builder and then use it to add advice and pointcut builders to build up a full aspect
   * definintion programatically.
   *
   * @param aspectClass
   * @param scope
   * @param containerClassName
   * @return a newly registered aspect builder
   */
  public AspectDefinitionBuilder newAspectBuilder(String aspectClass, DeploymentModel scope, String containerClassName);

  /**
   * Creates and adds a new mixin builder to the deployment set.
   *
   * @param aspectClass
   * @param deploymentModel
   * @param pointcut
   */
  public void addMixin(String aspectClass, DeploymentModel deploymentModel, String pointcut, boolean isTransient);

  /**
   * Returns class loader
   */
  public ClassLoader getClassLoader();

}

