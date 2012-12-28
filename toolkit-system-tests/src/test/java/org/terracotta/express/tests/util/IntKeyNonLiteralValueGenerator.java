/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.util;

public class IntKeyNonLiteralValueGenerator implements KeyValueGenerator<Integer, TCInt> {

  @Override
  public Integer getKey(int i) {
    return i;
  }

  @Override
  public TCInt getValue(int i) {
    return new TCInt(i);
  }

}
