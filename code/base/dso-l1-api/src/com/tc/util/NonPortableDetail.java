/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.io.Serializable;

public class NonPortableDetail implements Serializable {
  private final String label;
  private final String value;

  public NonPortableDetail(String label, String value) {
    this.label = label;
    this.value = value;
  }
  
  public String getLabel() {
    return label;
  }
  
  public String getValue() {
    return value;
  }
}
