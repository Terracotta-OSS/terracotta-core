/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.toolkit.api.tests;

import org.terracotta.express.tests.map.ClusteredMapDiffSetTest.MyInt;

public class NonLiteralKeyLiteralValueGenerator implements KeyValueGenerator {

  @Override
  public MyInt getKey(int i) {
    return new MyInt(i);

  }

  @Override
  public String getValue(int i) {
    return "Value-" + i;
  }

}
