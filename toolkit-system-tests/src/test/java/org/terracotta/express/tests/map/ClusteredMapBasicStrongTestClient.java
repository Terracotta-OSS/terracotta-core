/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.toolkit.store.ToolkitStoreConfigFields;

public class ClusteredMapBasicStrongTestClient extends AbstractClusteredMapBasicTestClient {

  public ClusteredMapBasicStrongTestClient(String[] args) {
    super(ToolkitStoreConfigFields.Consistency.STRONG, args);
  }

}