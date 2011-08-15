/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.logging.TCLogger;
import com.tc.object.dna.api.DNAEncoding;

public class PartialHashMapApplicator extends HashMapApplicator {

  public PartialHashMapApplicator(DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }

  /*
   * This applicator is to be used where the Map supports partial collection. (ex. HashMap)
   */
  @Override
  protected Object getObjectForValue(ApplicatorObjectManager objectManager, Object v) {
    return v;
  }

}
