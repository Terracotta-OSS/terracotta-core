/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import java.util.Arrays;

/**
 * Represents the <code>beans</code> config element hierarchy
 */
public class SpringContextBean {

  private String   name;
  private String[] nonDistributedFields;

  public SpringContextBean(String name, String[] nonDistributedFields) {
    this.name = name;
    this.nonDistributedFields = nonDistributedFields;
  }

  public String name() {
    return name;
  }

  public String[] nonDistributedFields() {
    return nonDistributedFields;
  }
  
  public String toString() {
    return "BEAN: " + name + "\nFIELDS:\n\n" + Arrays.asList(nonDistributedFields);
  }
}
