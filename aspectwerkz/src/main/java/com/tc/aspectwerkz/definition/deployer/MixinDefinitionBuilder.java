/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.aspectwerkz.definition.deployer;

import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.definition.DefinitionParserHelper;
import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;

/**
 * TODO document class
 *
 * @author Jonas Bon&#233;r
 */
public class MixinDefinitionBuilder implements DefinitionBuilder {
  private final ClassInfo m_classInfo;
  private final DeploymentModel m_deploymentModel;
  private final String m_pointcut;
  private final SystemDefinition m_systemDef;
  private boolean m_transient;

  public MixinDefinitionBuilder(final String mixinClass,
                                final DeploymentModel deploymentModel,
                                final String pointcut,
                                final boolean isTransient,
                                final SystemDefinition systemDef,
                                final ClassLoader loader) {
    m_classInfo = AsmClassInfo.getClassInfo(mixinClass, loader);
    m_deploymentModel = deploymentModel;
    m_pointcut = pointcut;
    m_transient = isTransient;
    m_systemDef = systemDef;
  }

  public void build() {
    DefinitionParserHelper.createAndAddMixinDefToSystemDef(
            m_classInfo, m_pointcut, m_deploymentModel, m_transient, m_systemDef
    );
  }
}
