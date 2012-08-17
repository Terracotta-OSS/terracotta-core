/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;

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
