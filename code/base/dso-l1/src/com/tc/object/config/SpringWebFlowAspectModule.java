/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.definition.deployer.AspectDefinitionBuilder;
import com.tc.aspectwerkz.definition.deployer.AspectModule;
import com.tc.aspectwerkz.definition.deployer.AspectModuleDeployer;


/**
 * Manages deployment of all AW aspects used for Spring WebFlow support
 *
 * @author Eugene Kuleshov
 */
public class SpringWebFlowAspectModule implements AspectModule {

  public void deploy(final AspectModuleDeployer deployer) {
    buildDefinitionForConversationLockProtocol(deployer);
  }

  private void buildDefinitionForConversationLockProtocol(AspectModuleDeployer deployer) {
    AspectDefinitionBuilder builder = deployer.newAspectBuilder("com.tcspring.ConversationLockProtocol", 
                                                                DeploymentModel.PER_JVM, null);

    builder.addAdvice("around",
      "execution(* org.springframework.webflow.conversation.impl.ConversationLockFactory.createLock())",
      "replaceUtilConversationLock(StaticJoinPoint jp)");
  }

}
