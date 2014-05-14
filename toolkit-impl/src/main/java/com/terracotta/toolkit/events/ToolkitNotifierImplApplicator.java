/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.events;

import com.tc.logging.TCLogger;
import com.tc.object.ClientObjectManager;
import com.tc.object.LogicalOperation;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.applicator.BaseApplicator;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;

import java.io.IOException;
import java.util.Arrays;

public class ToolkitNotifierImplApplicator extends BaseApplicator {

  public ToolkitNotifierImplApplicator(DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }

  @Override
  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object pojo) throws IOException,
      ClassNotFoundException {
    ToolkitNotifierImpl clusteredNotifierImpl = (ToolkitNotifierImpl) pojo;
    DNACursor cursor = dna.getCursor();
    while (cursor.next(encoding)) {
      Object action = cursor.getAction();
      if (action instanceof LogicalAction) {
        LogicalAction la = (LogicalAction) action;
        if (LogicalOperation.CLUSTERED_NOTIFIER.equals(la.getLogicalOperation())) {
          Object[] parameters = la.getParameters();
          if (parameters.length != 2) { throw new AssertionError(
                                                                 "ClusteredNotifier should have 2 parameters, but found: "
                                                                     + parameters.length + " : "
                                                                     + Arrays.asList(parameters)); }

          clusteredNotifierImpl.onNotification((String) parameters[0], (String) parameters[1]);
        } else if (LogicalOperation.DESTROY.equals(la.getLogicalOperation())) {
          clusteredNotifierImpl.applyDestroy();
        }
      } else if (action instanceof PhysicalAction) { throw new AssertionError(
                                                                              "ClusteredNotifier should not broadcast any physical actions"); }
    }
  }

  @Override
  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    // do nothing
  }

  @Override
  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    return addTo;
  }

  @Override
  public Object getNewInstance(ClientObjectManager objectManager, DNA dna) {
    throw new UnsupportedOperationException();
  }

}
