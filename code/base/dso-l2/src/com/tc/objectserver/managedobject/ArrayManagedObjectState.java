/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import org.apache.commons.lang.ArrayUtils;

import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.PhysicalManagedObjectFacade;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ArrayManagedObjectState extends LogicalManagedObjectState implements PrettyPrintable {

  private Object        arrayData;
  private int           size = DNA.NULL_ARRAY_SIZE;
  private boolean       isPrimitive;
  private LiteralValues literalType;

  ArrayManagedObjectState(long classID) {
    super(classID);
  }

  protected ArrayManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  public void apply(ObjectID objectID, DNACursor cursor, ApplyTransactionInfo includeIDs) throws IOException {
    ManagedObjectChangeListener listener = getListener();

    while (cursor.next()) {
      PhysicalAction a = cursor.getPhysicalAction();

      if (a.isArrayElement()) {
        int index = a.getArrayIndex();
        Object value = a.getObject();
        informListener(objectID, listener, index, value, includeIDs);
        setArrayElement(arrayData, index, value, literalType);
      } else if (a.isEntireArray()) {
        initArray(a.getObject());
        if (!isPrimitive) {
          Object[] objArray = (Object[]) arrayData;
          for (int i = 0, n = size; i < n; i++) {
            informListener(objectID, listener, i, objArray[i], includeIDs);
          }
        }
      } else if (a.isSubArray()) {
        int startPos = a.getArrayIndex();
        Object value = a.getObject();
        int length = Array.getLength(value);
        informListener(objectID, listener, startPos, length, value, includeIDs);
        System.arraycopy(value, 0, arrayData, startPos, length);
      } else {
        throw Assert.failure("unknown action type");
      }
    }
  }
  /**
   * This method returns whether this ManagedObjectState can have references or not.
   * @ return true : The Managed object represented by this state object will never have any reference to other objects.
   *         false : The Managed object represented by this state object can have references to other objects. 
   */
  @Override
  public boolean hasNoReferences() {
    return isPrimitive;
  }

  void initArray(Object array) {
    arrayData = array;
    size = Array.getLength(arrayData);
    Class clazz = arrayData.getClass().getComponentType();
    literalType = LiteralValues.valueForClassName(clazz.getName());
    isPrimitive = clazz.isPrimitive();
  }

  private static void setArrayElement(Object array, int index, Object value, LiteralValues type) {
    switch (type) {
      case BOOLEAN:
        ((boolean[]) array)[index] = ((Boolean) value).booleanValue();
        break;
      case BYTE:
        ((byte[]) array)[index] = ((Byte) value).byteValue();
        break;
      case CHARACTER:
        ((char[]) array)[index] = ((Character) value).charValue();
        break;
      case DOUBLE:
        ((double[]) array)[index] = ((Double) value).doubleValue();
        break;
      case FLOAT:
        ((float[]) array)[index] = ((Float) value).floatValue();
        break;
      case INTEGER:
        ((int[]) array)[index] = ((Integer) value).intValue();
        break;
      case LONG:
        ((long[]) array)[index] = ((Long) value).longValue();
        break;
      case SHORT:
        ((short[]) array)[index] = ((Short) value).shortValue();
        break;
      default:
        ((Object[]) array)[index] = value;
        break;
    }
  }

  /*
   * This method should be called before the new value is applied
   */
  private void informListener(ObjectID objectID, ManagedObjectChangeListener listener, int startPos, int length,
                              Object value, ApplyTransactionInfo includeIDs) {
    if (!isPrimitive) {
      Object[] oldArray = (Object[]) arrayData;
      Object[] newArray = (Object[]) value;
      for (int i = 0; i < length; i++) {
        Object oldVal = oldArray[startPos + i];
        Object newVal = newArray[i];
        ObjectID oldValue = oldVal instanceof ObjectID ? (ObjectID) oldVal : ObjectID.NULL_ID;
        ObjectID newValue = newVal instanceof ObjectID ? (ObjectID) newVal : ObjectID.NULL_ID;
        listener.changed(objectID, oldValue, newValue);
        includeIDs.addBackReference(newValue, objectID);
      }
    }
  }

  private void informListener(ObjectID objectID, ManagedObjectChangeListener listener, int index, Object value,
                              ApplyTransactionInfo includeIDs) {
    if (!isPrimitive) {
      Object[] objectArray = (Object[]) arrayData;
      Object oldVal = objectArray[index];
      ObjectID oldValue = oldVal instanceof ObjectID ? (ObjectID) oldVal : ObjectID.NULL_ID;
      ObjectID newValue = value instanceof ObjectID ? (ObjectID) value : ObjectID.NULL_ID;
      listener.changed(objectID, oldValue, newValue);
      includeIDs.addBackReference(newValue, objectID);
    }
  }

  protected void addAllObjectReferencesTo(Set refs) {
    if (!isPrimitive) {
      addAllObjectReferencesFromIteratorTo(Arrays.asList((Object[]) arrayData).iterator(), refs);
    }
  }

  /*
   * This method is overridden to give Arrays ability to be partial in L1
   */
  public void addObjectReferencesTo(ManagedObjectTraverser traverser) {
    if (!isPrimitive) {
      traverser.addReachableObjectIDs(getObjectReferences());
    }
  }

  public void dehydrate(ObjectID objectID, DNAWriter writer, DNAType type) {
    writer.setArrayLength(size);
    writer.addEntireArray(arrayData);
  }

  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    if (limit < 0) {
      limit = size;
    } else {
      limit = Math.min(limit, size);
    }

    Map dataCopy = new HashMap(limit);

    for (int i = 0; i < limit; i++) {
      // XXX: Yuck...don't use reflection (need to separate primitive and object array state impls first through)
      dataCopy.put(String.valueOf(i), Array.get(arrayData, i));
    }

    boolean isArray = true;
    ObjectID parent = ObjectID.NULL_ID;
    boolean isInner = false;

    return new PhysicalManagedObjectFacade(objectID, parent, className, dataCopy, isInner, size /*limit*/, isArray);
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    PrettyPrinter rv = out;
    out = out.print(getClass().getName()).duplicateAndIndent().println();
    out.indent().print("size: " + size).println();
    out.indent().print("data: " + ArrayUtils.toString(arrayData)).println();
    return rv;
  }

  public byte getType() {
    return ARRAY_TYPE;
  }

  protected void basicWriteTo(ObjectOutput out) throws IOException {
    out.writeObject(arrayData);
  }

  static ArrayManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    ArrayManagedObjectState amos = new ArrayManagedObjectState(in);
    amos.initArray(in.readObject());
    return amos;
  }

  protected boolean basicEquals(LogicalManagedObjectState other) {
    ArrayManagedObjectState amo = (ArrayManagedObjectState) other;
    if (!(size == amo.size && isPrimitive == amo.isPrimitive)) return false;
    if (isPrimitive) {
      return literalType == amo.literalType && equals(arrayData, amo.arrayData, literalType);
    } else {
      // DNAEncordingImpl decodes all non primitive array into Object[]
      return (literalType == LiteralValues.OBJECT || amo.literalType == LiteralValues.OBJECT)
        && equals(arrayData, amo.arrayData, LiteralValues.OBJECT);
    }
  }

  private static boolean equals(Object a1, Object a2, LiteralValues type) {
    switch (type) {
      case BOOLEAN:
        return Arrays.equals((boolean[]) a1, (boolean[]) a2);
      case BYTE:
        return Arrays.equals((byte[]) a1, (byte[]) a2);
      case CHARACTER:
        return Arrays.equals((char[]) a1, (char[]) a2);
      case DOUBLE:
        return Arrays.equals((double[]) a1, (double[]) a2);
      case FLOAT:
        return Arrays.equals((float[]) a1, (float[]) a2);
      case INTEGER:
        return Arrays.equals((int[]) a1, (int[]) a2);
      case LONG:
        return Arrays.equals((long[]) a1, (long[]) a2);
      case SHORT:
        return Arrays.equals((short[]) a1, (short[]) a2);
      default:
        return Arrays.equals((Object[]) a1, (Object[]) a2);
    }
  }

}
