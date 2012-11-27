/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.util;

public interface KeyValueGenerator {
  Object getKey(int i);

  Object getValue(int i);

}
