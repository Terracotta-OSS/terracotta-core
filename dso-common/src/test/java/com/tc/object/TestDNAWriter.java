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
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.PhysicalAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TestDNAWriter implements DNAWriter {
  private final List actions = new ArrayList();

  public TestDNAWriter() {
    //
  }

  @Override
  public void setIgnoreMissing(final boolean ignoreMissing) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void addLogicalAction(final LogicalOperation method, final Object[] parameters, LogicalChangeID logicalChangeID) {
    this.actions.add(new LogicalAction(method, parameters, logicalChangeID));
  }

  @Override
  public void addPhysicalAction(final String field, final Object value) {
    addPhysicalAction(field, value, value instanceof ObjectID);
  }

  public void finalizeDNA(final boolean isDeltaDNA) {
    //
  }

  public void finalizeDNA(final boolean isDeltaDNA, final int actionCount, final int totalLength) {
    //
  }

  @Override
  public void addArrayElementAction(final int index, final Object value) {
    this.actions.add(new PhysicalAction(value, index));
  }

  @Override
  public void addEntireArray(final Object value) {
    this.actions.add(new PhysicalAction(value));
  }

  @Override
  public void addLiteralValue(final Object value) {
    this.actions.add(new LiteralAction(value));
  }

  @Override
  public void setParentObjectID(final ObjectID id) {
    //
  }

  @Override
  public void setArrayLength(final int length) {
    //
  }

  @Override
  public void addPhysicalAction(final String fieldName, final Object value, final boolean canBeReference) {
    this.actions.add(new PhysicalAction(fieldName, value, canBeReference));
  }

  @Override
  public int getActionCount() {
    return this.actions.size();
  }

  public boolean containsAction(final Object targetAction) {
    if (targetAction instanceof LogicalAction) {
      return containsLogicalAction((LogicalAction) targetAction);
    } else if (targetAction instanceof PhysicalAction) {
      return containsPhysicalAction((PhysicalAction) targetAction);
    } else if (targetAction instanceof LiteralAction) { return containsLiteralAction((LiteralAction) targetAction); }

    return false;
  }

  public boolean containsLogicalAction(final LogicalAction targetAction) {
    for (final Iterator i = this.actions.iterator(); i.hasNext();) {
      final Object action = i.next();
      if (!(action instanceof LogicalAction)) {
        continue;
      }
      final LogicalAction logicalAction = (LogicalAction) action;
      if (identicalLogicalAction(targetAction, logicalAction)) { return true; }
    }
    return false;
  }

  public boolean containsPhysicalAction(final PhysicalAction targetAction) {
    for (final Iterator i = this.actions.iterator(); i.hasNext();) {
      final Object action = i.next();
      if (!(action instanceof PhysicalAction)) {
        continue;
      }
      final PhysicalAction physicalAction = (PhysicalAction) action;
      if (identicalPhysicalAction(targetAction, physicalAction)) { return true; }
    }
    return false;
  }

  public boolean containsLiteralAction(final LiteralAction targetAction) {
    for (final Iterator i = this.actions.iterator(); i.hasNext();) {
      final Object action = i.next();
      if (!(action instanceof LiteralAction)) {
        continue;
      }
      final LiteralAction literalAction = (LiteralAction) action;
      if (identicalLiteralAction(targetAction, literalAction)) { return true; }
    }
    return false;
  }

  private boolean identicalLiteralAction(final LiteralAction a1, final LiteralAction a2) {
    if (a1 == null || a2 == null) { return false; }
    if (a1.getObject() == null || a2.getObject() == null) { return false; }

    return a1.getObject().equals(a2.getObject());
  }

  private boolean identicalPhysicalAction(final PhysicalAction a1, final PhysicalAction a2) {
    if (a1 == null || a2 == null) { return false; }

    if (!a1.isEntireArray() && !a2.isEntireArray()) {
      if (a1.getFieldName() == null || a2.getFieldName() == null) { return false; }
    }

    if (a1.isEntireArray() != a2.isEntireArray()) { return false; }

    if (a1.getObject() == null && a2.getObject() == null) { return true; }
    if (a1.getObject() == null && a2.getObject() != null) { return false; }
    if (a1.getObject() != null && a2.getObject() == null) { return false; }

    if (a1.isEntireArray()) {
      return compareArrays(a1.getObject(), a2.getObject());
    } else if (a1.getObject() instanceof Object[] && a2.getObject() instanceof Object[]) {
      return compareArrays(a1.getObject(), a2.getObject());
    } else {
      if (a1.getFieldName().equals(a2.getFieldName())) { return (a1.getObject().equals(a2.getObject())); }
    }
    return false;
  }

  private boolean compareArrays(Object o1, Object o2) {
    if (o1 instanceof boolean[]) { return Arrays.equals((boolean[]) o1, (boolean[]) o2); }
    if (o1 instanceof byte[]) { return Arrays.equals((byte[]) o1, (byte[]) o2); }
    if (o2 instanceof char[]) { return Arrays.equals((char[]) o1, (char[]) o2); }
    if (o2 instanceof double[]) { return Arrays.equals((double[]) o1, (double[]) o2); }
    if (o2 instanceof float[]) { return Arrays.equals((float[]) o1, (float[]) o2); }
    if (o2 instanceof int[]) { return Arrays.equals((int[]) o1, (int[]) o2); }
    if (o2 instanceof long[]) { return Arrays.equals((long[]) o1, (long[]) o2); }
    if (o2 instanceof short[]) { return Arrays.equals((short[]) o1, (short[]) o2); }

    return Arrays.equals((Object[]) o1, (Object[]) o2);
  }

  private boolean identicalLogicalAction(final LogicalAction a1, final LogicalAction a2) {
    if (a1 == null || a2 == null) { return false; }
    if (a1.getParameters() == null || a2.getParameters() == null) { return false; }

    if (a1.getLogicalOperation()== a2.getLogicalOperation()) {
      if (a1.getParameters().length == a2.getParameters().length) {
        for (int i = 0; i < a1.getParameters().length; i++) {
          if (!a1.getParameters()[i].equals(a2.getParameters()[i])) { return false; }
        }
        return true;
      }
    }
    return false;
  }

  public void addClassLoaderAction(final String classLoaderFieldName, final ClassLoader value) {
    actions.add(new PhysicalAction(classLoaderFieldName, value, false));
  }

  @Override
  public void addSubArrayAction(final int start, final Object array, final int length) {
    actions.add(new PhysicalAction(array, start));
  }

  @Override
  public void copyTo(final TCByteBufferOutput dest) {
    throw new ImplementMe();

  }

  @Override
  public DNAWriter createAppender() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void finalizeHeader() {
    //
  }

  @Override
  public boolean isContiguous() {
    return true;
  }

  @Override
  public void markSectionEnd() {
    //
  }

  public TestDNACursor getDNACursor() {
    return new TestDNACursor(this.actions);
  }

  @Override
  public void addLogicalAction(LogicalOperation method, Object[] parameters) {
    this.actions.add(new LogicalAction(method, parameters));

  }
}
