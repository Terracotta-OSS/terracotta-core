/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.util.Assert;
import com.tc.util.stringification.OurStringBuilder;

public class NamedLock implements Lock {

  private final String    lockName;
  private final String    methodExpression;
  private final LockLevel lockLevel;

  public NamedLock(String lockName, String methodExpression, LockLevel lockLevel) {
    Assert.assertNotBlank(lockName);
    Assert.assertNotBlank(methodExpression);

    this.lockName = lockName;
    this.methodExpression = methodExpression;
    this.lockLevel = lockLevel;
  }

  public boolean isAutoLock() {
    return false;
  }

  public String lockName() {
    return this.lockName;
  }

  public String methodExpression() {
    return this.methodExpression;
  }

  public LockLevel lockLevel() {
    return this.lockLevel;
  }

  public boolean equals(Object that) {
    if (!(that instanceof NamedLock)) return false;
    NamedLock thatLock = (NamedLock) that;
    return new EqualsBuilder().append(this.methodExpression, thatLock.methodExpression).append(this.lockLevel,
                                                                                               thatLock.lockLevel)
        .append(this.lockName, thatLock.lockName).isEquals();
  }

  public int hashCode() {
    return new HashCodeBuilder().append(this.methodExpression).append(this.lockLevel).append(this.lockName)
        .toHashCode();
  }

  public String toString() {
    return new OurStringBuilder(this, OurStringBuilder.COMPACT_STYLE)
        .append("method expression", this.methodExpression).append("lock level", this.lockLevel).append("lock name",
                                                                                                        this.lockName)
        .toString();
  }

}
