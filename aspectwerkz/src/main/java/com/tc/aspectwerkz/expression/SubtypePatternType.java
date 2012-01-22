/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.expression;

import java.io.Serializable;

/**
 * Type safe enum for the different matching types.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class SubtypePatternType implements Serializable {

  public static final SubtypePatternType NOT_HIERARCHICAL = new SubtypePatternType("NOT_HIERARCHICAL");

  public static final SubtypePatternType MATCH_ON_ALL_METHODS = new SubtypePatternType("MATCH_ON_ALL_METHODS");

  public static final SubtypePatternType MATCH_ON_BASE_TYPE_METHODS_ONLY = new SubtypePatternType(
          "MATCH_ON_BASE_TYPE_METHODS_ONLY"
  );

  private final String myName;

  /**
   * Creates a new instance
   *
   * @param name
   */
  private SubtypePatternType(final String name) {
    myName = name;
  }

  /**
   * Returns the string representation.
   *
   * @return the string representation
   */
  public String toString() {
    return myName;
  }
}
