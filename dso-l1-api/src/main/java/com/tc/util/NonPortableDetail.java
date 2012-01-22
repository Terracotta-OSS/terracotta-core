/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import java.io.Serializable;

/**
 * Label/value pair for use in a {@link NonPortableReason}.
 */
public class NonPortableDetail implements Serializable {
  private final String label;
  private final String value;

  /**
   * @param label Label
   * @param value Value
   */
  public NonPortableDetail(String label, String value) {
    this.label = label;
    this.value = value;
  }
  
  /**
   * @return The label
   */
  public String getLabel() {
    return label;
  }
  
  /**
   * @return The value
   */
  public String getValue() {
    return value;
  }
}
