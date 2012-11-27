/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.util;

public class IntIntKeyValueGenerator implements KeyValueGenerator {

  @Override
  public Integer getKey(int i) {
    return i;
  }

  @Override
  public Integer getValue(int i) {
    return i;
  }

}
