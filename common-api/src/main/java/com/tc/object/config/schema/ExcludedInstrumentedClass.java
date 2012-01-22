/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.util.Assert;
import com.tc.util.stringification.OurStringBuilder;

/**
 * An {@link InstrumentedClass} that represents an excluded class.
 */
public class ExcludedInstrumentedClass implements InstrumentedClass {

  private final String classExpression;

  public ExcludedInstrumentedClass(String classExpression) {
    Assert.assertNotBlank(classExpression);

    this.classExpression = classExpression;
  }

  public boolean isInclude() {
    return false;
  }

  public String classExpression() {
    return this.classExpression;
  }

  public boolean honorTransient() {
    throw Assert.failure("Honor-transient has no meaning on excluded classes");
  }
  
  public boolean honorVolatile() {
    throw Assert.failure("Honor-volatile has no meaning on excluded classes");
  }

  public boolean callConstructorOnLoad() {
    throw Assert.failure("Call-constructor-on-load has no meaning on excluded classes");
  }

  public IncludeOnLoad onLoad() {
    throw Assert.failure("Call-constructor-on-load has no meaning on excluded classes");
  }

  public String toString() {
    return new OurStringBuilder(this, OurStringBuilder.COMPACT_STYLE).append("classExpression", this.classExpression)
        .toString();
  }
}
