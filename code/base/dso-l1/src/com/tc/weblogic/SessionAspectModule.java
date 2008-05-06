/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.weblogic;

import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.definition.deployer.AspectDefinitionBuilder;
import com.tc.aspectwerkz.definition.deployer.AspectModule;
import com.tc.aspectwerkz.definition.deployer.AspectModuleDeployer;

public class SessionAspectModule implements AspectModule {

  public void deploy(AspectModuleDeployer deployer) {
    if (WeblogicHelper.isWL9()) {
      addWL9Aspect(deployer);
    } else if (WeblogicHelper.isWL8()) {
      addWL8Aspect(deployer);
    }
  }

  private void addWL8Aspect(AspectModuleDeployer deployer) {
    AspectDefinitionBuilder builder = deployer.newAspectBuilder("com.tc.weblogic.SessionAspect",
                                                                DeploymentModel.PER_JVM, null);
    builder
        .addAdvice("around", "withincode(* weblogic.servlet.internal.WebAppServletContext.prepareFromDescriptors(..)) "
                             + "AND call(* weblogic.management.descriptors.webapp.WebAppDescriptorMBean.getFilters()) "
                             + "AND this(webAppServletContext)",
                   "addFilterIfNeeded(StaticJoinPoint jp, weblogic.servlet.internal.WebAppServletContext webAppServletContext)");

    builder
        .addAdvice(
                   "around",
                   "withincode(* weblogic.servlet.internal.WebAppServletContext.prepareFromDescriptors(..)) "
                       + "AND call(* weblogic.management.descriptors.webapp.WebAppDescriptorMBean.getFilterMappings()) "
                       + "AND this(webAppServletContext)",
                   "addFilterMappingIfNeeded(StaticJoinPoint jp, weblogic.servlet.internal.WebAppServletContext webAppServletContext)");
  }

  private void addWL9Aspect(AspectModuleDeployer deployer) {
    AspectDefinitionBuilder builder = deployer.newAspectBuilder("com.tc.weblogic.SessionAspectWL9",
                                                                DeploymentModel.PER_JVM, null);

    builder.addAdvice("around", "withincode(* weblogic.servlet.internal.FilterManager.registerServletFilters(..)) "
                                + "AND call(* weblogic.j2ee.descriptor.WebAppBean+.getFilters()) "
                                + "AND this(filterManager)",
                      "addFilterIfNeeded(StaticJoinPoint jp, weblogic.servlet.internal.FilterManager filterManager)");

    builder
        .addAdvice("around", "withincode(* weblogic.servlet.internal.FilterManager.registerServletFilters(..)) "
                             + "AND call(* weblogic.j2ee.descriptor.WebAppBean+.getFilterMappings()) "
                             + "AND this(filterManager)",
                   "addFilterMappingIfNeeded(StaticJoinPoint jp, weblogic.servlet.internal.FilterManager filterManager)");

  }
}
