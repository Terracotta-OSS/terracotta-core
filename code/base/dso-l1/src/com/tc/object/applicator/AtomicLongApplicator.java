/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.logging.TCLogger;
import com.tc.object.TCObjectExternal;
import com.tc.object.TraversedReferences;
import com.tc.object.bytecode.AtomicLongAdapter;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.util.Assert;
import com.tc.util.runtime.Vm;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * NOTE: This applicator is only used for IBM JDK
 */
public class AtomicLongApplicator extends BaseApplicator {
  public AtomicLongApplicator(DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
    Vm.assertIsIbm();
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    addTo.addAnonymousReference(pojo);
    return addTo;
  }

  public void hydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNA dna, Object po)
      throws IOException, IllegalArgumentException, ClassNotFoundException {
    DNACursor cursor = dna.getCursor();

    Assert.assertTrue(po.getClass().getName(), po instanceof AtomicLong);

    // You can get multiple actions for an AtomicLong if txn get folded in the client
    while (cursor.next(encoding)) {
      PhysicalAction a = (PhysicalAction) cursor.getAction();
      Long value = (Long) a.getObject();
      ((AtomicLong) po).set(value.longValue());
    }
  }

  public void dehydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNAWriter writer, Object pojo) {
    AtomicLong ai = (AtomicLong) pojo;
    long value = ai.get();

    writer.addPhysicalAction(AtomicLongAdapter.VALUE_FIELD_NAME, Long.valueOf(value));
  }

  public Object getNewInstance(ApplicatorObjectManager objectManager, DNA dna) {
    throw new UnsupportedOperationException();
  }

}
