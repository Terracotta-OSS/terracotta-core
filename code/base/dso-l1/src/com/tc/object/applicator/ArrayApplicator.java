/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.TCObjectExternal;
import com.tc.object.TraversedReferences;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;

import java.io.IOException;
import java.lang.reflect.Array;

public class ArrayApplicator extends BaseApplicator {

  public ArrayApplicator(DNAEncoding encoding) {
    super(encoding, TCLogging.getLogger(ArrayApplicator.class));
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    if (ClassUtils.isPrimitiveArray(pojo)) { return addTo; }

    Object[] array = (Object[]) pojo;

    for (Object o : array) {
      if (o != null && isPortableReference(o.getClass())) {
        addTo.addAnonymousReference(o);
      }
    }
    return addTo;
  }

  public void hydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNA dna, Object po)
      throws IOException, IllegalArgumentException, ClassNotFoundException {
    DNACursor cursor = dna.getCursor();

    while (cursor.next(encoding)) {
      PhysicalAction a = cursor.getPhysicalAction();

      if (a.isArrayElement()) {
        setArrayElement(a.getArrayIndex(), a.getObject(), tcObject, po);
      } else if (a.isEntireArray() || a.isSubArray()) {
        Object array = a.getObject();
        int offset = a.isEntireArray() ? 0 : a.getArrayIndex();

        if (ClassUtils.isPrimitiveArray(array)) {
          System.arraycopy(array, 0, po, offset, Array.getLength(array));
        } else {
          hydrateNonPrimitiveArray((Object[]) array, tcObject, po, offset);
        }
      } else {
        throw Assert.failure("Invalid physical action for array");
      }
    }
  }

  private static void hydrateNonPrimitiveArray(Object[] source, TCObjectExternal tcObject, Object pojo, int offset) {
    for (int i = 0, n = source.length; i < n; i++) {
      setArrayElement(offset + i, source[i], tcObject, pojo);
    }
  }

  private static void setArrayElement(int index, Object value, TCObjectExternal tcObject, Object pojo) {
    String fieldName = String.valueOf(index);
    if (value instanceof ObjectID) {
      tcObject.setArrayReference(index, (ObjectID) value);
    } else {
      tcObject.clearReference(fieldName);
      // if you're trying to get rid of reflection here, you need to make sure you deal properly with
      // Wrapper objects (like Integer, Long, etc)
      Array.set(pojo, index, value);
    }
  }

  public void dehydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNAWriter writer, Object pojo) {
    writer.setArrayLength(Array.getLength(pojo));

    if (ClassUtils.isPrimitiveArray(pojo)) {
      writer.addEntireArray(pojo);
    } else {
      Object[] array = (Object[]) pojo;
      Object[] toEncode = new Object[array.length];

      // convert to array of literals and ObjectID
      for (int i = 0, n = array.length; i < n; i++) {
        Object element = array[i];
        if (!objectManager.isPortableInstance(element)) {
          toEncode[i] = ObjectID.NULL_ID;
          continue;
        }

        final Object obj = getDehydratableObject(element, objectManager);
        if (obj == null) {
          toEncode[i] = ObjectID.NULL_ID;
        } else {
          toEncode[i] = obj;
        }
      }

      writer.addEntireArray(toEncode);
    }
  }

  public Object getNewInstance(ApplicatorObjectManager objectManager, DNA dna) {
    throw new UnsupportedOperationException();
  }

}
