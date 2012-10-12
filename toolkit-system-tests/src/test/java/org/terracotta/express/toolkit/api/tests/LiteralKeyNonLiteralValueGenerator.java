/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;


public class LiteralKeyNonLiteralValueGenerator implements KeyValueGenerator {


  @Override
  public String getKey(int i) {
    return "key-" + i;

  }

  @Override
  public MyInt getValue(int i) {
    return new MyInt(i);
  }

}
