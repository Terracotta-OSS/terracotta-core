/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.aspect;

/**
 * Interface for that all mixin factories must implement.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public interface MixinFactory {

  /**
   * Creates a new perJVM mixin instance, if it already exists then return it.
   *
   * @return the mixin instance
   */
  Object mixinOf();

  /**
   * Creates a new perClass mixin instance, if it already exists then return it.
   *
   * @param klass
   * @return the mixin instance
   */
  Object mixinOf(Class klass);

  /**
   * Creates a new perInstance mixin instance, if it already exists then return it.
   *
   * @param instance
   * @return the mixin instance
   */
  Object mixinOf(Object instance);
}