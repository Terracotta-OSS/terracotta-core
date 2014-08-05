/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import com.tc.logging.TCLogger;
import com.tc.object.ClientObjectManager;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.platform.PlatformService;

public class ToolkitSortedMapImplApplicator extends ToolkitMapImplApplicator {

  public ToolkitSortedMapImplApplicator(DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }

  @Override
  public Object getNewInstance(ClientObjectManager objectManager, DNA dna, PlatformService platformService) {
    return new ToolkitSortedMapImpl(platformService);
  }
}
