/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.test;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;

/**
 * Allows you to build valid config for an instrumented class. This class <strong>MUST NOT</strong> invoke the actual
 * XML beans to do its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class InstrumentedClassConfigBuilderImpl extends BaseConfigBuilder implements InstrumentedClassConfigBuilder {

  private boolean isInclude;

  public InstrumentedClassConfigBuilderImpl(Class clazz) {
    this();
    setClassExpression(clazz.getName());
  }

  public InstrumentedClassConfigBuilderImpl() {
    //super(4, ALL_PROPERTIES);
    super(5, ALL_PROPERTIES);
    this.isInclude = true;
    
    setArrayPropertyTagName("on-load", "method");
  }

  public void setIsInclude(boolean isInclude) {
    this.isInclude = isInclude;
  }

  public void setClassExpression(String value) {
    if (isInclude) setProperty("class-expression", value);
    else setProperty("exclude", value);
  }

  public void setHonorTransient(String value) {
    setProperty("honor-transient", value);
  }

  public void setHonorTransient(boolean value) {
    setProperty("honor-transient", value);
  }

  public void setCallConstructorOnLoad(String value) {
    setProperty("call-constructor-on-load", value);
  }

  public void setCallConstructorOnLoad(boolean value) {
    setProperty("call-constructor-on-load", value);
  }
  
  public void setCallMethodOnLoad(String value) {
    setProperty("on-load", new Object[]{value});
  }

  private static final String[] ALL_PROPERTIES = new String[] { "class-expression", "honor-transient",
      "call-constructor-on-load", "exclude", "on-load"              };

  public String toString() {
    String out = "";

    if (this.isInclude) {
      out += indent() + openElement("include");
      out += indent() + elements(ALL_PROPERTIES);
      out += indent() + closeElement("include");
    } else {
      out += indent() + elements(ALL_PROPERTIES);
    }

    return out;
  }

}
