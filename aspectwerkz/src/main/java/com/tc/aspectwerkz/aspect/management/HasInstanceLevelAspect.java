/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.aspect.management;

/**
 * Interface applied to a target class when it has instance level aspects (perInstance, perThis, perTarget)
 * <p/>
 * Should <b>NEVER</b> be implemented by the user, but is applied to target classes by the weaver.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public interface HasInstanceLevelAspect {

  /**
   * Returns the instance level aspect given a specific aspect factory class, since we know that one aspect class
   * has one or more factory (due to qNames) and one factory acts for only one aspect qName.
   *
   * @param aspectFactoryClass
   * @return the aspect instance or null if no such aspect
   */
  Object aw$getAspect(Class aspectFactoryClass);

  /**
   * Cheks if the instance level aspect with the specific factory class was associated with the instance.
   *
   * @param aspectFactoryClass
   * @return true in case the aspect was registers, false otherwise
   */
  boolean aw$hasAspect(Class aspectFactoryClass);

  Object aw$bindAspect(Class aspectFactoryClass, Object aspect);
}
