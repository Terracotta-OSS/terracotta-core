/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.test.schema;

import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.util.Assert;

/**
 * Allows you to build valid config for a lock. This class <strong>MUST NOT</strong> invoke the actual XML beans to do
 * its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class LockConfigBuilderImpl extends BaseConfigBuilder implements LockConfigBuilder {

  private final String       tag;

  public LockConfigBuilderImpl(String tag, Class clazz, String lockLevel) {
    this(tag);
    setMethodExpression("* " + clazz.getName() + ".*(..)");
    setLockLevel(lockLevel);
  }
  
  public LockConfigBuilderImpl(String tag) {
    super(4, ALL_PROPERTIES);

    Assert.assertNotBlank(tag);
    this.tag = tag;
  }

  public void setLockName(String value) {
    setProperty("lock-name", value);
  }

  public void setMethodExpression(String value) {
    setProperty("method-expression", value);
  }

  public void setLockLevel(String value) {
    setProperty("lock-level", value);
  }

  public void setLockName(int value) {
    setProperty("lock-name", value);
  }

  private static final String[] ALL_PROPERTIES = new String[] { "lock-name", "method-expression", "lock-level" };

  public String toString() {
    return openElement(this.tag) + elements(ALL_PROPERTIES) + closeElement(this.tag);
  }

}
