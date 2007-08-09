/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.object.ClientObjectManager;
import com.tc.object.dna.api.IDNAEncoding;

public class PartialHashMapApplicator extends HashMapApplicator {

  public PartialHashMapApplicator(IDNAEncoding encoding) {
    super(encoding);
  }

  /*
   * This applicator is to be used where the Map supports partial collection. (ex. HashMap)
   */
  protected Object getObjectForValue(ClientObjectManager objectManager, Object v) {
    return v;
  }

}
