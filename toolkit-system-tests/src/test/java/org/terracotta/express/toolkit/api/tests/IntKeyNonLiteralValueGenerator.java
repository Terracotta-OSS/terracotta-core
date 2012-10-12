/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;
public class IntKeyNonLiteralValueGenerator implements KeyValueGenerator {

  @Override
  public Integer getKey(int i) {
    return i;
  }

  @Override
  public MyInt getValue(int i) {
    return new MyInt(i);
  }

}
