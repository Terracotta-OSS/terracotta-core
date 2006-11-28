/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
