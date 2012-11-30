/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.util;

public class NonLiteralKeyNonLiteralValueGenerator implements KeyValueGenerator {
  @Override
  public TCInt getKey(int i) {
    return new TCInt(i);

  }

  @Override
  public TCInt getValue(int i) {
    return new TCInt(i);
  }

}
