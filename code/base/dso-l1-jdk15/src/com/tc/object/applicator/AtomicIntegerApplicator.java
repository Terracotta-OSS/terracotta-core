/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.object.ClientObjectManager;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.bytecode.AtomicIntegerAdapter;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.util.Assert;
import com.tc.util.runtime.Vm;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NOTE: This applicator is only used for IBM JDK
 */
public class AtomicIntegerApplicator extends BaseApplicator {
  public AtomicIntegerApplicator(DNAEncoding encoding) {
    super(encoding);
    Vm.assertIsIbm();
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    addTo.addAnonymousReference(pojo);
    return addTo;
  }

  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object po) throws IOException,
      IllegalArgumentException, ClassNotFoundException {
    DNACursor cursor = dna.getCursor();

    Assert.assertTrue(po.getClass().getName(), po instanceof AtomicInteger);

    // You can get multiple actions for an AtomicInteger if txn get folded in the client
    while (cursor.next(encoding)) {
      PhysicalAction a = cursor.getPhysicalAction();
      Integer value = (Integer) a.getObject();
      ((AtomicInteger) po).set(value.intValue());
    }
  }

  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    AtomicInteger ai = (AtomicInteger) pojo;
    int value = ai.get();

    writer.addPhysicalAction(AtomicIntegerAdapter.VALUE_FIELD_NAME, new Integer(value));
  }

  public Object getNewInstance(ClientObjectManager objectManager, DNA dna) {
    throw new UnsupportedOperationException();
  }

}
