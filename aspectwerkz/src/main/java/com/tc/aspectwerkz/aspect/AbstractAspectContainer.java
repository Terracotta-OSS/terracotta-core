/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.aspect;


import com.tc.aspectwerkz.AspectContext;
import com.tc.aspectwerkz.definition.AspectDefinition;
import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.definition.SystemDefinitionContainer;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;


/**
 * Abstract base class for an aspect container implementations that is passing an AspectContext when
 * creating the aspect instance if there is such a single arg constructor in the aspect.
 * <p/>
 * Provides support for getting the AspectDefinition.
 * <p/>
 * Can be used as a base class for user defined container.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public abstract class AbstractAspectContainer implements AspectContainer {

  protected Class m_aspectClass;
  protected Reference m_classLoader;
  protected String m_uuid;
  protected String m_qualifiedName;
  private AspectDefinition m_aspectDefinitionLazy;

  public static final int ASPECT_CONSTRUCTION_TYPE_UNKNOWN = 0;
  public static final int ASPECT_CONSTRUCTION_TYPE_DEFAULT = 1;
  public static final int ASPECT_CONSTRUCTION_TYPE_ASPECT_CONTEXT = 2;
  public static final Object[] EMPTY_OBJECT_ARRAY = new Object[]{};

  /**
   * The aspect construction type. Defaults to unknown
   */
  protected int m_constructionType = ASPECT_CONSTRUCTION_TYPE_UNKNOWN;

  /**
   * Create a new container
   *
   * @param aspectClass
   * @param aopSystemClassLoader the classloader of the defining system (not necessary the one of the aspect class)
   * @param uuid
   * @param qualifiedName
   * @param parameters
   */
  public AbstractAspectContainer(Class aspectClass, ClassLoader aopSystemClassLoader, String uuid, String qualifiedName, Map parameters) {
    m_aspectClass = aspectClass;// we hold a strong ref this the container is hold by the aspect factory only
    m_classLoader = new WeakReference(aopSystemClassLoader);
    m_uuid = uuid;
    m_qualifiedName = qualifiedName;
  }

  /**
   * Lazy getter for the aspect definition
   *
   * @return the aspect definition
   */
  public AspectDefinition getAspectDefinition() {
    if (m_aspectDefinitionLazy == null) {
      SystemDefinition def = SystemDefinitionContainer.getDefinitionFor(getDefiningSystemClassLoader(), m_uuid);
      if (def == null) {
        throw new RuntimeException(
                "Definition " + m_uuid + " not found from " + getDefiningSystemClassLoader()
        );
      }
      for (Iterator iterator = def.getAspectDefinitions().iterator(); iterator.hasNext();) {
        AspectDefinition aspectDefinition = (AspectDefinition) iterator.next();
        if (m_qualifiedName.equals(aspectDefinition.getQualifiedName())) {
          m_aspectDefinitionLazy = aspectDefinition;
        }
      }
      if (m_aspectDefinitionLazy == null) {
        throw new RuntimeException(
                "Aspect definition not found " + m_qualifiedName + " from " + getDefiningSystemClassLoader()
        );
      }
    }
    return m_aspectDefinitionLazy;
  }

  /**
   * @return the classloader of the system defining the aspect handled by that container instance
   */
  public ClassLoader getDefiningSystemClassLoader() {
    return ((ClassLoader) m_classLoader.get());
  }

  public Object aspectOf() {
    return createAspect(getContext(null));
  }

  public Object aspectOf(Class klass) {
    return createAspect(getContext(klass));
  }

  public Object aspectOf(Object instance) {
    return createAspect(getContext(instance));
  }

  public Object aspectOf(Thread thread) {
    return createAspect(getContext(thread));
  }

  /**
   * To be implemented by the concrete aspect containers.
   * <p/>
   * Should return a new aspect instance.
   *
   * @return a new aspect instance
   */
  protected abstract Object createAspect(AspectContext aspectContext);

  /**
   * Create a new AspectContext associated with the given instance (class/instance/thread/null)
   *
   * @param associated
   * @return the context
   */
  protected AspectContext getContext(Object associated) {
    AspectDefinition aspectDefinition = getAspectDefinition();
    return new AspectContext(
            m_uuid,
            m_aspectClass,
            aspectDefinition.getName(),
            aspectDefinition.getDeploymentModel(),
            aspectDefinition,
            aspectDefinition.getParameters(),
            associated
    );
  }

  /**
   * Returns the aspect class
   *
   * @return the aspect class
   */
  public Class getAspectClass() {
    return m_aspectClass;
  }
}