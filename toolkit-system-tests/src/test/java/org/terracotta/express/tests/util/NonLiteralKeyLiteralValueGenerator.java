/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.util;

public class NonLiteralKeyLiteralValueGenerator implements KeyValueGenerator<TCInt, String> {

  @Override
  public TCInt getKey(int i) {
    return new TCInt(i);

  }

  @Override
  public String getValue(int i) {
    return "Value-" + i;
  }

}
