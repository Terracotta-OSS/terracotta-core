/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.util;

public interface ClusteredStringBuilderFactory {

  /**
   * Gets an already created ClusteredStringBuilder with 'name' or creates one if not already created
   */
  ClusteredStringBuilder getClusteredStringBuilder(String name);

}
