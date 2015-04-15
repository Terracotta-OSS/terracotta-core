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
package com.terracotta.toolkit.object.serialization;

import com.tc.logging.TCLogger;
import com.tc.object.ClientObjectManager;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.applicator.BaseApplicator;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.platform.PlatformService;

import java.io.IOException;

public class SerializedClusterObjectImplApplicator extends BaseApplicator {

  public SerializedClusterObjectImplApplicator(final DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }

  @Override
  public void dehydrate(final ClientObjectManager objectManager, final TCObject tco, final DNAWriter writer,
                        final Object pojo) {
    writer.addEntireArray(asSerializedClusterObject(pojo).getBytes());
  }

  private static SerializedClusterObjectImpl asSerializedClusterObject(final Object pojo) {
    SerializedClusterObjectImpl serializedClusterObject = (SerializedClusterObjectImpl) pojo;
    return serializedClusterObject;
  }

  @Override
  public Object getNewInstance(final ClientObjectManager objectManager, final DNA dna, PlatformService platformService) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TraversedReferences getPortableObjects(final Object pojo, final TraversedReferences addTo) {
    return addTo;
  }

  @Override
  public void hydrate(final ClientObjectManager objectManager, final TCObject tco, final DNA dna, final Object pojo)
      throws IOException, ClassNotFoundException {
    synchronized (pojo) {
      DNACursor cursor = dna.getCursor();

      while (cursor.next(encoding)) {
        PhysicalAction a = cursor.getPhysicalAction();
        if (a.isEntireArray()) {
          asSerializedClusterObject(pojo).internalSetValue((byte[]) a.getObject());
        } else {
          throw new IllegalArgumentException("Extra physical action - " + a);
        }
      }
    }
  }
}
