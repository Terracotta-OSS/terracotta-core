/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.tc.aspectwerkz.AspectContext;
import com.tc.aspectwerkz.aspect.AbstractAspectContainer;

import java.util.Map;

/**
 * Spring custom aspect container, grabs the aspects from the bean factory.
 * 
 * @author Jonas Bon&#233;r
 */
public final class SpringAspectContainer extends AbstractAspectContainer {

  public static final String       BEAN_FACTORY_KEY = "bean-factory";

  private final ApplicationContext m_beanFactory;

  /**
   * Create a new Spring aspect container.
   * 
   * @param aspectClass
   * @param aopSystemClassLoader the classloader of the defining system (not necessary the one of the aspect class)
   * @param uuid
   * @param qualifiedName
   * @param parameters
   */
  public SpringAspectContainer(final Class aspectClass, final ClassLoader aopSystemClassLoader, final String uuid,
                               final String qualifiedName, final Map parameters) {
    super(aspectClass, aopSystemClassLoader, uuid, qualifiedName, parameters);
    String beanConfig = (String) parameters.get(BEAN_FACTORY_KEY);
    if (beanConfig == null) { throw new AssertionError(); }

    m_beanFactory = new ClassPathXmlApplicationContext(beanConfig);
  }

  /**
   * Creates a new aspect by getting it from the spring bean factory for the proxy.
   * 
   * @param aspectContext
   * @return an aspect instance
   */
  protected Object createAspect(final AspectContext aspectContext) {
    return m_beanFactory.getBean(aspectContext.getName());
  }
}
