/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.exception.TCNotSupportedMethodException;
import com.tc.object.ClientObjectManager;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.IDNAEncoding;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.Map;

public class LiteralTypesApplicator extends BaseApplicator {

  public LiteralTypesApplicator(TCClass clazz, IDNAEncoding encoding) {
    super(encoding);
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    // if (pojo instanceof TransparentAccess) { throw new AssertionError("Instance of Literal Type: " + clazz.getName()
    // + " should not implement TransparentAccess."); }
    return addTo;
  }

  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object po) throws IOException,
      ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    Assert.eval(cursor.getActionCount() <= 1);

    if (cursor.next(encoding)) {
      LiteralAction a = (LiteralAction) cursor.getAction();
      Object value = a.getObject();

      tcObject.setLiteralValue(value);
    }
  }
  
  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    if (!objectManager.isPortableInstance(pojo)) { return; }
    writer.addLiteralValue(pojo);
  }

  public Object getNewInstance(ClientObjectManager objectManager, DNA dna) throws IOException, ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    Assert.assertEquals(1, cursor.getActionCount());

    cursor.next(encoding);
    LiteralAction a = (LiteralAction) cursor.getAction();
    Object value = a.getObject();

    return value;
  }

  public Map connectedCopy(Object source, Object dest, Map visited, ClientObjectManager objectManager,
                           OptimisticTransactionManager txManager) {
    throw new TCNotSupportedMethodException();
  }
}
