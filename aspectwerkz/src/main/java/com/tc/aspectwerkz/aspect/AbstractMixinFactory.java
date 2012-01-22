/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.aspect;

import java.lang.reflect.Constructor;


import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.exception.DefinitionException;

/**
 * Abstract base class for the mixin container implementations.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public abstract class AbstractMixinFactory implements MixinFactory {

  protected final Class m_mixinClass;
  protected final DeploymentModel m_deploymentModel;
  protected Constructor m_defaultConstructor;
  protected Constructor m_perClassConstructor;
  protected Constructor m_perInstanceConstructor;

  /**
   * Creates a new mixin factory.
   *
   * @param mixinClass
   * @param deploymentModel
   */
  public AbstractMixinFactory(final Class mixinClass, final DeploymentModel deploymentModel) {
    m_mixinClass = mixinClass;
    m_deploymentModel = deploymentModel;
    try {
      if (m_deploymentModel.equals(DeploymentModel.PER_CLASS)) {
        m_perClassConstructor = m_mixinClass.getConstructor(new Class[]{Class.class});
      } else if (m_deploymentModel.equals(DeploymentModel.PER_INSTANCE)) {
        m_perInstanceConstructor = m_mixinClass.getConstructor(new Class[]{Object.class});
      } else if (m_deploymentModel.equals(DeploymentModel.PER_JVM)) {
        m_defaultConstructor = m_mixinClass.getConstructor(new Class[]{});
      } else {
        throw new DefinitionException(
                "deployment model for [" + m_mixinClass.getName() + "] is not supported [" +
                        m_deploymentModel + "]"
        );
      }
    } catch (NoSuchMethodException e1) {
      try {
        m_defaultConstructor = m_mixinClass.getConstructor(new Class[]{});
      } catch (NoSuchMethodException e2) {
        throw new DefinitionException(
                "mixin [" + m_mixinClass.getName() +
                        "] does not have a constructor that matches with its deployment model or a non-argument default constructor"
        );
      }
    }
  }

  /**
   * Creates a new perJVM mixin instance.
   *
   * @return the mixin instance
   */
  public abstract Object mixinOf();

  /**
   * Creates a new perClass mixin instance.
   *
   * @param klass
   * @return the mixin instance
   */
  public abstract Object mixinOf(Class klass);

  /**
   * Creates a new perInstance mixin instance.
   *
   * @param instance
   * @return the mixin instance
   */
  public abstract Object mixinOf(Object instance);
}
