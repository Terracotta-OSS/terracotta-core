/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.util;


public class LiteralKeyNonLiteralValueGenerator implements KeyValueGenerator {


  @Override
  public String getKey(int i) {
    return "key-" + i;

  }

  @Override
  public TCInt getValue(int i) {
    return new TCInt(i);
  }

}
