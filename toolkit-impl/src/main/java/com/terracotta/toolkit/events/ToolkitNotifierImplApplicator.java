/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
import com.tc.platform.PlatformService;

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
  public Object getNewInstance(ClientObjectManager objectManager, DNA dna, PlatformService platformService) {
    return new ToolkitNotifierImpl(platformService);
  }

}
