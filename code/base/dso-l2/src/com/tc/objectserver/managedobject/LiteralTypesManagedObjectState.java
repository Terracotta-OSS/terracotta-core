/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.exception.TCRuntimeException;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.dna.impl.ClassInstance;
import com.tc.object.dna.impl.ClassLoaderInstance;
import com.tc.object.dna.impl.EnumInstance;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.object.loaders.Namespace;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.PhysicalManagedObjectFacade;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.text.PrettyPrinterImpl;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LiteralTypesManagedObjectState extends AbstractManagedObjectState implements PrettyPrintable {
  private Object reference;

  LiteralTypesManagedObjectState() {
    super();
  }

  public int hashCode() {
    throw new TCRuntimeException("Don't hash me!");
  }

  public void apply(ObjectID objectID, DNACursor cursor, BackReferences includeIDs) throws IOException {
    while (cursor.next()) {
      LiteralAction a = (LiteralAction) cursor.getAction();
      // PhysicalAction a = cursor.getPhysicalAction();
      // Assert.assertTrue(a.isLiteralType());
      reference = a.getObject();
    }
  }

  public void dehydrate(ObjectID objectID, DNAWriter writer) {
    writer.addLiteralValue(reference);
  }

  public String toString() {
    // XXX: Um... this is gross.
    StringWriter writer = new StringWriter();
    PrintWriter pWriter = new PrintWriter(writer);
    new PrettyPrinterImpl(pWriter).visit(this);
    return writer.getBuffer().toString();
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    PrettyPrinter rv = out;
    out = out.print(getClass().getName()).duplicateAndIndent().println();
    out.indent().print("references: " + reference).println();
    out.indent().print("listener: " + getListener()).println();
    return rv;
  }

  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    // NOTE: limit is ignored for literal object facades

    Map dataCopy = new HashMap();
    dataCopy.put(className, reference);

    return new PhysicalManagedObjectFacade(objectID, ObjectID.NULL_ID, className, dataCopy, false, DNA.NULL_ARRAY_SIZE,
                                           false);
  }

  public Set getObjectReferences() {
    return Collections.EMPTY_SET;
  }

  public void addObjectReferencesTo(ManagedObjectTraverser traverser) {
    // Do Nothing
  }

  public byte getType() {
    return LITERAL_TYPE;
  }

  public void writeTo(ObjectOutput o) throws IOException {
    o.writeObject(reference);
  }

  static LiteralTypesManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    LiteralTypesManagedObjectState lmos = new LiteralTypesManagedObjectState();
    lmos.reference = in.readObject();
    return lmos;
  }

  protected boolean basicEquals(AbstractManagedObjectState o) {
    LiteralTypesManagedObjectState lmos = (LiteralTypesManagedObjectState) o;
    return reference.equals(lmos.reference);
  }

  public String getClassName() {
    Assert.assertNotNull(reference);

    if (reference instanceof ClassInstance) {
      return "java.lang.Class";
    } else if (reference instanceof ClassLoaderInstance) {
      return "java.lang.ClassLoader";
    } else if (reference instanceof UTF8ByteDataHolder) {
      return "java.lang.String";
    } else if (reference instanceof EnumInstance) { return "java.lang.Enum"; }

    return reference.getClass().getName();
  }

  public String getLoaderDescription() {
    return Namespace.getStandardBootstrapLoaderName();
  }

}
