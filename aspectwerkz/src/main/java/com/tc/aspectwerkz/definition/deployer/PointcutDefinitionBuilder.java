/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.aspectwerkz.definition.deployer;

import com.tc.aspectwerkz.definition.AspectDefinition;
import com.tc.aspectwerkz.definition.DefinitionParserHelper;

/**
 * TODO document class
 *
 * @author Jonas Bon&#233;r
 */
public class PointcutDefinitionBuilder implements DefinitionBuilder {
  private final String m_name;
  private final String m_expression;
  private final AspectDefinition m_aspectDef;

  public PointcutDefinitionBuilder(final String name, final String expression, final AspectDefinition aspectDef) {
    m_name = name;
    m_expression = expression;
    m_aspectDef = aspectDef;
  }

  public void build() {
    DefinitionParserHelper.createAndAddPointcutDefToAspectDef(
            m_name, m_expression, m_aspectDef
    );
  }
}
