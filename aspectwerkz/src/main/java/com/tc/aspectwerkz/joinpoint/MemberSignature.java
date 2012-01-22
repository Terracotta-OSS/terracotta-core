/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint;

import com.tc.backport175.Annotation;

/**
 * Interface for the member signatures (method, constructor and field).
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public interface MemberSignature extends Signature {

  /**
   * Return the given annotation if any.
   *
   * @param annotationClass the annotation class
   * @return the annotation or null
   */
  Annotation getAnnotation(Class annotationClass);

  /**
   * Return all the annotations.
   *
   * @return annotations
   */
  Annotation[] getAnnotations();
}
