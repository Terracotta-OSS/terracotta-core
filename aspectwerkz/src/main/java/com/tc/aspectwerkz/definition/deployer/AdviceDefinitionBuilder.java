/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

package com.tc.aspectwerkz.definition.deployer;

import com.tc.aspectwerkz.definition.AspectDefinition;
import com.tc.aspectwerkz.definition.DefinitionParserHelper;
import com.tc.aspectwerkz.reflect.MethodInfo;

/**
 * TODO document class
 *
 * @author Jonas Bon&#233;r
 */
public class AdviceDefinitionBuilder implements DefinitionBuilder {
  private final String m_fqn;
  private final String m_type;
  private final String m_pointcut;
  private final MethodInfo m_method;
  private final AspectDefinition m_aspectDef;

  public AdviceDefinitionBuilder(final String fqn,
                                 final String type,
                                 final String pointcut,
                                 final MethodInfo method,
                                 final AspectDefinition aspectDef) {
    m_fqn = fqn;
    m_type = type;
    m_pointcut = pointcut;
    m_method = method;
    m_aspectDef = aspectDef;
  }

  public void build() {
    DefinitionParserHelper.createAndAddAdviceDefsToAspectDef(
            m_type, m_pointcut, m_fqn, m_method, m_aspectDef
    );
  }
}
