/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.util;

public class LiteralKeyLiteralValueGenerator implements KeyValueGenerator {

  @Override
  public String getKey(int i) {
    return "key-" + i;

  }

  @Override
  public String getValue(int i) {
    return "Value-" + i;
  }

}
