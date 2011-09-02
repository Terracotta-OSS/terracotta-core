/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.util.Assert;
import com.tc.util.stringification.OurStringBuilder;

/**
 * An {@link InstrumentedClass} that represents an included class.
 */
public class IncludedInstrumentedClass implements InstrumentedClass {

  private final String        classExpression;
  private final boolean       honorTransient;
  private final boolean       honorVolatile;
  private final IncludeOnLoad onLoad;

  public IncludedInstrumentedClass(String classExpression, boolean honorTransient, boolean honorVolatile, IncludeOnLoad onLoad) {
    Assert.assertNotBlank(classExpression);

    this.classExpression = classExpression;
    this.honorTransient = honorTransient;
    this.honorVolatile = honorVolatile;
    this.onLoad = onLoad;
  }

  public boolean isInclude() {
    return true;
  }

  public String classExpression() {
    return this.classExpression;
  }

  public boolean honorTransient() {
    return this.honorTransient;
  }

  public boolean honorVolatile() {
    return this.honorVolatile;
  }

  public IncludeOnLoad onLoad() {
    return this.onLoad;
  }

  public String toString() {
    return new OurStringBuilder(this, OurStringBuilder.COMPACT_STYLE).append("classExpression", this.classExpression)
        .append("honorTransient", this.honorTransient).append("honorVolatile", this.honorVolatile).append("onLoad", this.onLoad).toString();
  }
}
