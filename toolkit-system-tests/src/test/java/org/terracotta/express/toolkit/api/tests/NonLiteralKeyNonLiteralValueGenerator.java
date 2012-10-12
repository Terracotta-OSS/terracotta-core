/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;

public class NonLiteralKeyNonLiteralValueGenerator implements KeyValueGenerator {
  @Override
  public MyInt getKey(int i) {
    return new MyInt(i);

  }

  @Override
  public MyInt getValue(int i) {
    return new MyInt(i);
  }

}
