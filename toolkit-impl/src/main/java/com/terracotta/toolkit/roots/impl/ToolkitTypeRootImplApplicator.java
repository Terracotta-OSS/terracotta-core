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
package com.terracotta.toolkit.roots.impl;

import com.tc.logging.TCLogger;
import com.tc.object.ClientObjectManager;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.applicator.BaseApplicator;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.platform.PlatformService;

import java.io.IOException;

public class ToolkitTypeRootImplApplicator extends BaseApplicator {

  public ToolkitTypeRootImplApplicator(DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }

  @Override
  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object pojo) throws IOException,
      ClassNotFoundException {
    final DNACursor cursor = dna.getCursor();

    while (cursor.next(this.encoding)) {
      final LogicalAction action = cursor.getLogicalAction();
      final LogicalOperation method = action.getLogicalOperation();
      final Object[] params = action.getParameters();
      apply(objectManager, pojo, method, params);
    }
  }

  private void apply(final ClientObjectManager objectManager, final Object po, final LogicalOperation method, final Object[] params) {
    final ToolkitTypeRootImpl m = (ToolkitTypeRootImpl) po;
    switch (method) {
      case PUT:
        Object k = params[0];
        Object v = params[1];
        m.applyAdd((String) k, (ObjectID) v);
        break;
      case REMOVE:
        k = params[0];
        m.applyRemove((String) k);
        break;
      default:
        throw new AssertionError("invalid action:" + method);
    }
  }

  @Override
  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    // We dont need to add anything in dehydrate here
    // This is called only when a new object is sent to the server
  }

  @Override
  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    // No other instances
    return addTo;
  }

  @Override
  public Object getNewInstance(ClientObjectManager objectManager, DNA dna, PlatformService platformService) {
    return new ToolkitTypeRootImpl(platformService);
  }
}
