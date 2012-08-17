/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import com.tc.logging.TCLogger;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TraversedReferences;
import com.tc.object.applicator.HashSetApplicator;
import com.tc.object.dna.api.DNAEncoding;

import java.util.Set;

public class ToolkitSetImplApplicator extends HashSetApplicator {
  public ToolkitSetImplApplicator(DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }

  @Override
  protected void apply(ClientObjectManager objectManager, Set set, int method, Object[] params)
      throws ClassNotFoundException {
    ToolkitSetImpl internalSet = (ToolkitSetImpl) set;
    switch (method) {
      case SerializationUtil.ADD:
      case SerializationUtil.REMOVE:
        Object v = params[0];
        Object value = v instanceof ObjectID ? objectManager.lookupObject((ObjectID) v) : v;
        internalSet.internalMutate(method, value);
        break;
      case SerializationUtil.CLEAR:
        internalSet.internalClear();
        break;
      case SerializationUtil.DESTROY:
        internalSet.applyDestroy();
        break;
      default:
        throw new AssertionError("invalid action:" + method);
    }
  }

  @Override
  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    return addTo;
  }

}
