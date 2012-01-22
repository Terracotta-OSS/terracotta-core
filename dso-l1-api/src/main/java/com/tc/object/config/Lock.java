/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.tc.util.Assert;

/**
 * representation of a lock from the config file
 */
public class Lock {

  private String         methodJoinPointExpression;
  private LockDefinition lockDefinition;

  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public Lock(String methodJoinPointExpression, LockDefinition lockDefinition) {
    Assert.assertNotNull(lockDefinition);

    this.methodJoinPointExpression = methodJoinPointExpression;
    this.lockDefinition = lockDefinition;
  }

  public String getMethodJoinPointExpression() {
    return this.methodJoinPointExpression;
  }

  public LockDefinition getLockDefinition() {
    return this.lockDefinition;
  }

}
