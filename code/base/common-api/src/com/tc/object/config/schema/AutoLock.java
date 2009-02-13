/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.util.Assert;
import com.tc.util.stringification.OurStringBuilder;

public class AutoLock implements Lock {

  private final String    methodExpression;
  private final LockLevel lockLevel;

  public AutoLock(String methodExpression, LockLevel lockLevel) {
    Assert.assertNotBlank(methodExpression);
    Assert.assertNotNull(lockLevel);

    this.methodExpression = methodExpression;
    this.lockLevel = lockLevel;
  }

  public boolean isAutoLock() {
    return true;
  }

  public String lockName() {
    throw Assert.failure("Autolocks don't have names.");
  }

  public String methodExpression() {
    return this.methodExpression;
  }

  public LockLevel lockLevel() {
    return this.lockLevel;
  }

  public boolean equals(Object that) {
    if (!(that instanceof AutoLock)) return false;
    AutoLock thatLock = (AutoLock) that;
    return new EqualsBuilder().append(this.methodExpression, thatLock.methodExpression).append(this.lockLevel,
                                                                                               thatLock.lockLevel)
        .isEquals();
  }

  public int hashCode() {
    return new HashCodeBuilder().append(this.methodExpression).append(this.lockLevel).toHashCode();
  }

  public String toString() {
    return new OurStringBuilder(this, OurStringBuilder.COMPACT_STYLE)
        .append("method expression", this.methodExpression).append("lock level", this.lockLevel).toString();
  }

}
