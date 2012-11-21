/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;



public class StringKeyValueGenerator {

  public String getKey(int i) {
    return "key-" + i;
  }
  public String getValue(int i) {
    return "value-" + i;
  }

}
