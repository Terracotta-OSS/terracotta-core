/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.object.TestDNACursor;
import com.tc.object.TestDNAWriter;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class LiteralTypesManagedObjectStateTest extends AbstractTestManagedObjectState {

  public void basicLiteral(final String className, final Object obj) throws Exception {
    final TestDNACursor cursor = new TestDNACursor();
    final ManagedObjectState state = createManagedObjectState(className, cursor);
    cursor.addLiteralAction(obj);
    state.apply(this.objectID, cursor, new ApplyTransactionInfo());

    // API verification
    Assert.assertTrue(state.getObjectReferences().size() == 0);
    Assert.assertTrue(state.getType() == ManagedObjectState.LITERAL_TYPE);
    Assert.assertEquals(state.getClassName(), className);

    // dehydrate
    cursor.reset();
    cursor.next();
    final Object action = cursor.getAction();
    final TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(this.objectID, dnaWriter, DNAType.L1_FAULT);
    Assert.assertTrue(dnaWriter.containsAction(action));

    // writeTo, readFrom and equal
    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    final TCObjectOutputStream out = new TCObjectOutputStream(bout);
    state.writeTo(out);
    final ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
    final TCObjectInputStream in = new TCObjectInputStream(bin);
    final ManagedObjectState state2 = ManagedObjectStateFactory.getInstance()
        .readManagedObjectStateFrom(in, ManagedObjectState.LITERAL_TYPE);
    Assert.assertTrue(state.equals(state2));
  }

  public void testLiteralInteger() throws Exception {
    basicLiteral("java.lang.Integer", Integer.valueOf(34));
  }

  public void testLiteralString() throws Exception {
    basicLiteral("java.lang.String", new String("Hello"));
  }

  public void testLiteralByte() throws Exception {
    final Byte data = Byte.valueOf((byte) 0xf0);
    basicLiteral(data.getClass().getName(), data);
  }

}
