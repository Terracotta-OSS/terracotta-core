/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.annotation;

import com.tc.backport175.Annotation;

import java.io.Serializable;

/**
 * Holds the annotation proxy instance and the name of the annotation.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class AnnotationInfo implements Serializable {

  /**
   * The fully qualified name.
   */
  private final String m_name;

  /**
   * The annotation proxy.
   */
  private final Annotation m_annotation;

  /**
   * Creates a new annotation info.
   *
   * @param name
   * @param annotation
   */
  public AnnotationInfo(final String name, final Annotation annotation) {
    m_name = name;
    m_annotation = annotation;
  }

  /**
   * Returns the FQN.
   *
   * @return
   */
  public String getName() {
    return m_name;
  }

  /**
   * Returns the annotation proxy.
   *
   * @return
   */
  public Annotation getAnnotation() {
    return m_annotation;
  }
}
