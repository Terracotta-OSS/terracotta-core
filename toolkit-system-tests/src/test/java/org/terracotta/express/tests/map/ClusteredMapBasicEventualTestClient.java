/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.toolkit.store.ToolkitStoreConfigFields;

public class ClusteredMapBasicEventualTestClient extends AbstractClusteredMapBasicTestClient {

  public ClusteredMapBasicEventualTestClient(String[] args) {
    super(ToolkitStoreConfigFields.Consistency.EVENTUAL, args);
  }

}