/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

package com.tc.aspectwerkz.definition.deployer;

import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.annotation.AspectAnnotationParser;
import com.tc.aspectwerkz.aspect.AdviceType;
import com.tc.aspectwerkz.definition.AspectDefinition;
import com.tc.aspectwerkz.definition.DefinitionParserHelper;
import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.transform.inlining.AspectModelManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * TODO document class
 *
 * @author Jonas Bon&#233;r
 */
public class AspectDefinitionBuilder implements DefinitionBuilder {
  private final List m_advices = new ArrayList();
  private final List m_pointcuts = new ArrayList();
  private final AspectDefinition m_aspectDef;
  private final ClassInfo m_classInfo;
  private final SystemDefinition m_systemDef;

  public AspectDefinitionBuilder(final String aspectClass,
                                 final DeploymentModel deploymentModel,
                                 final String containerClassName,
                                 final SystemDefinition systemDef,
                                 final ClassLoader loader) {
    m_classInfo = AsmClassInfo.getClassInfo(aspectClass, loader);
    m_aspectDef = new AspectDefinition(aspectClass, m_classInfo, systemDef);
    m_aspectDef.setDeploymentModel(deploymentModel);
    m_aspectDef.setContainerClassName(containerClassName);
    m_systemDef = systemDef;
    AspectModelManager.defineAspect(m_classInfo, m_aspectDef, loader);
    try {
      AspectAnnotationParser.parse(m_classInfo, m_aspectDef, loader);
    } catch(Throwable t) {
      System.err.println("### Unable to retrieve annotation data for "+aspectClass+"; "+t.toString());
    }
  }

  /**
   * @deprecated
   * FIXME remove this method - does not work with after returning and after throwing - use the one below
   */ 
  public void addAdvice(
      final AdviceType type,
      final String pointcut,
      final String fqn) {
    addAdvice(type.toString(), pointcut, fqn);
  }

  public void addAdvice(
      final String type,
      final String pointcut,
      final String fqn) {
    MethodInfo methodInfo = DefinitionParserHelper.createMethodInfoForAdviceFQN(fqn, m_aspectDef, m_classInfo);
    m_advices.add(new AdviceDefinitionBuilder(fqn, type, pointcut, methodInfo, m_aspectDef));
  }

  public void addPointcut(final String name, final String expression) {
    m_pointcuts.add(new PointcutDefinitionBuilder(name, expression, m_aspectDef));
  }

  public void build() {
    for (Iterator it = m_advices.iterator(); it.hasNext();) {
      ((DefinitionBuilder) it.next()).build();
    }
    for (Iterator it = m_pointcuts.iterator(); it.hasNext();) {
      ((DefinitionBuilder) it.next()).build();
    }
    m_systemDef.addAspect(m_aspectDef);
  }
  
  public AspectDefinition getAspectDefinition() {
    return m_aspectDef;
  }
}
