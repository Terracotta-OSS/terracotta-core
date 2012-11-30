/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
  public Object getNewInstance(final ClientObjectManager objectManager, final DNA dna) {
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
