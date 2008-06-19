/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.test;

public class TcPropertyBuilder extends BaseConfigBuilder{

  private static final String[] ALL_PROPERTIES = concat(new Object[]{"name", "value"});
  private String name;
  private String value;
  
  public TcPropertyBuilder() {
    super(1, ALL_PROPERTIES);
  }

  public TcPropertyBuilder(String name, String value){
    this();
    this.name = name;
    this.value = value;
    setProperty("name", name);
    setProperty("value", value);
  }
  
  public void setTcProperty(String name, String value){
    this.name = name;
    this.value = value;
    setProperty("name", name);
    setProperty("value", value);
  }
  
  public String toString() {
    String out = "";
    
    out += indent() + "<property " + (this.name != null ? "name=\"" + this.name + "\"" : "") + (this.value != null ? " value=\"" + this.value + "\"" : "") + "/>";
    
    return out;
  }
}
