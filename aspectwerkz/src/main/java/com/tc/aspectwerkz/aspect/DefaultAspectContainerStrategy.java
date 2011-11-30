/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.aspect;


import com.tc.aspectwerkz.AspectContext;
import com.tc.aspectwerkz.exception.WrappedRuntimeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Implements a sample aspect container strategy.
 * </p>
 * Use container="com.tc.aspectwerkz.aspect.DefaultAspectContainerStrategy" in the aop.xml
 * The aspect must then have a no-arg constructor or a single arg constructor with param "AspectContext".
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class DefaultAspectContainerStrategy extends AbstractAspectContainer {

  //FIXME - needs to have data structures for the aspects to handle cycle - current impl is old and BOGUS

  /**
   * The constructor for the aspect.
   */
  protected Constructor m_aspectConstructor = null;

  /**
   * Creates a new aspect container strategy.
   */
  public DefaultAspectContainerStrategy(Class aspectClass, ClassLoader aopSystemClassLoader, String uuid, String qualifiedName, Map parameters) {
    super(aspectClass, aopSystemClassLoader, uuid, qualifiedName, parameters);
  }

  /**
   * Creates a new aspect instance.
   *
   * @return the new aspect instance
   */
  protected Object createAspect(AspectContext aspectContext) {
    if (m_aspectConstructor == null) {
      m_aspectConstructor = findConstructor();
    }
    try {
      switch (m_constructionType) {
        case ASPECT_CONSTRUCTION_TYPE_DEFAULT:
          return m_aspectConstructor.newInstance(EMPTY_OBJECT_ARRAY);
        case ASPECT_CONSTRUCTION_TYPE_ASPECT_CONTEXT:
          return m_aspectConstructor.newInstance(new Object[]{aspectContext});
        default:
          throw new Error("should not happen");
      }
    } catch (InvocationTargetException e) {
      e.printStackTrace();
      throw new WrappedRuntimeException(e.getTargetException());
    } catch (Exception e) {
      throw new WrappedRuntimeException(e);
    }
  }

  /**
   * Grabs the correct constructor for the aspect.
   *
   * @return the constructor for the aspect
   */
  protected Constructor findConstructor() {
    Constructor aspectConstructor = null;
    Class aspectClass = getAspectClass();
    Constructor[] constructors = aspectClass.getDeclaredConstructors();
    for (int i = 0; i < constructors.length; i++) {
      Constructor constructor = constructors[i];
      Class[] parameterTypes = constructor.getParameterTypes();
      if (parameterTypes.length == 0) {
        m_constructionType = ASPECT_CONSTRUCTION_TYPE_DEFAULT;
        aspectConstructor = constructor;
      } else if ((parameterTypes.length == 1) && parameterTypes[0].equals(AspectContext.class)) {
        m_constructionType = ASPECT_CONSTRUCTION_TYPE_ASPECT_CONTEXT;
        aspectConstructor = constructor;
        break;
      }
    }
    if (m_constructionType == ASPECT_CONSTRUCTION_TYPE_UNKNOWN) {
      throw new RuntimeException(
              "aspect ["
                      + aspectClass.getName()
                      +
                      "] does not have a valid constructor (either default no-arg or one that takes a AspectContext type as its only parameter)"
                      + " to be used with container=\"com.tc.aspectwerkz.aspect.DefaultAspectContainerStrategy\""
      );
    }
    return aspectConstructor;
  }
}