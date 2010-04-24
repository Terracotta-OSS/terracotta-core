/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.logging.TCLogging;
import com.tc.object.TCClass;
import com.tc.object.TCObjectExternal;
import com.tc.object.TraversedReferences;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LiteralAction;
import com.tc.util.Assert;

import java.io.IOException;

public class LiteralTypesApplicator extends BaseApplicator {

  public LiteralTypesApplicator(TCClass clazz, DNAEncoding encoding) {
    super(encoding, TCLogging.getLogger(LiteralTypesApplicator.class));
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    // if (pojo instanceof TransparentAccess) { throw new AssertionError("Instance of Literal Type: " + clazz.getName()
    // + " should not implement TransparentAccess."); }
    return addTo;
  }

  public void hydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNA dna, Object po)
      throws IOException, ClassNotFoundException {
    DNACursor cursor = dna.getCursor();

    while (cursor.next(encoding)) {
      LiteralAction a = (LiteralAction) cursor.getAction();
      Object value = a.getObject();

      tcObject.setLiteralValue(value);
    }
  }

  public void dehydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNAWriter writer, Object pojo) {
    if (!objectManager.isPortableInstance(pojo)) { return; }
    writer.addLiteralValue(pojo);
  }

  public Object getNewInstance(ApplicatorObjectManager objectManager, DNA dna) throws IOException,
      ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    Assert.assertEquals(1, cursor.getActionCount());

    cursor.next(encoding);
    LiteralAction a = (LiteralAction) cursor.getAction();
    Object value = a.getObject();

    return value;
  }
}
