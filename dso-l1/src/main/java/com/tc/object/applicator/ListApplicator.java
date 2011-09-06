/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObjectExternal;
import com.tc.object.TraversedReferences;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class ListApplicator extends BaseApplicator {
  private static final TCLogger logger = TCLogging.getLogger(ListApplicator.class);

  public ListApplicator(DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    for (Iterator i = ((List) pojo).iterator(); i.hasNext();) {
      Object o = i.next();
      if (o != null && isPortableReference(o.getClass())) {
        addTo.addAnonymousReference(o);
      }
    }
    return addTo;
  }

  public void hydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNA dna, Object po)
      throws IOException, ClassNotFoundException {
    List list = (List) po;
    DNACursor cursor = dna.getCursor();

    while (cursor.next(encoding)) {
      LogicalAction action = cursor.getLogicalAction();
      int method = action.getMethod();
      Object[] params = action.getParameters();

      for (int i = 0, n = params.length; i < n; i++) {
        Object param = params[i];
        if (param instanceof ObjectID) {
          params[i] = objectManager.lookupObject((ObjectID) param);
        }
      }

      try {
        apply(list, method, params);
      } catch (IndexOutOfBoundsException ioobe) {
        logger.error("Error applying update to " + po, ioobe);
      }
    }
  }

  private void apply(List list, int method, Object[] params) {
    final int size = list.size();
    switch (method) {
      case SerializationUtil.ADD:
      case SerializationUtil.ADD_LAST:
        list.add(params[0]);
        break;
      case SerializationUtil.INSERT_AT:
      case SerializationUtil.ADD_AT:
        int aaindex = ((Integer) params[0]).intValue();
        if (aaindex > size) {
          logger.error("Inserting at index " + size + " instead of requested index " + aaindex
                       + "because list is only of size " + size);
          aaindex = size;
        }
        list.add(aaindex, params[1]);
        break;
      case SerializationUtil.ADD_FIRST:
        list.add(0, params[0]);
        break;
      case SerializationUtil.SET_ELEMENT:
      case SerializationUtil.SET:
        int sindex = ((Integer) params[0]).intValue();
        if (sindex >= size) {
          logger.error("Cannot set element at index " + sindex + " because object is only of size " + size);
          return;
        }
        list.set(sindex, params[1]);
        break;
      case SerializationUtil.REMOVE:
        list.remove(params[0]);
        break;
      case SerializationUtil.REMOVE_AT:
        int raindex = ((Integer) params[0]).intValue();
        if (raindex >= size) {
          logger.error("Cannot remove element at index " + raindex + " because object is only of size " + size);
          return;
        }
        list.remove(raindex);
        break;
      case SerializationUtil.REMOVE_RANGE:
        int fromIndex = ((Integer) params[0]).intValue();
        if (fromIndex >= size) {
          logger.error("Cannot remove element at index " + fromIndex + " because object is only of size " + size);
          return;
        }
        int toIndex = ((Integer) params[1]).intValue();
        if (toIndex > size) {
          logger.error("Cannot remove element at index " + (toIndex - 1) + " because object is only of size " + size);
          return;
        }
        int removeIndex = fromIndex;
        while (fromIndex++ < toIndex) {
          list.remove(removeIndex);
        }
        break;
      case SerializationUtil.REMOVE_FIRST:
        if (size > 0) {
          list.remove(0);
        } else {
          logger.error("Cannot removeFirst() because list is empty");
        }
        break;
      case SerializationUtil.REMOVE_LAST:
        if (size > 0) {
          list.remove(list.size() - 1);
        } else {
          logger.error("Cannot removeLast() because list is empty");
        }
        break;
      case SerializationUtil.REMOVE_ALL:
        for (Object o : params) {
          list.remove(o);
        }
        break;
      case SerializationUtil.CLEAR:
        list.clear();
        break;
      case SerializationUtil.SET_SIZE:
        int setSize = ((Integer) params[0]).intValue();
        ((Vector) list).setSize(setSize);
        break;
      case SerializationUtil.TRIM_TO_SIZE:
        // do nothing for now
        break;
      default:
        throw new AssertionError("invalid action:" + method);
    }
  }

  public void dehydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNAWriter writer, Object pojo) {
    List list = (List) pojo;

    for (int i = 0; i < list.size(); i++) {
      Object value = list.get(i);
      if (!objectManager.isPortableInstance(value)) {
        continue;
      }

      final Object addValue = getDehydratableObject(value, objectManager);
      if (addValue == null) {
        continue;
      }
      writer.addLogicalAction(SerializationUtil.ADD, new Object[] { addValue });
    }
  }

  public Object getNewInstance(ApplicatorObjectManager objectManager, DNA dna) {
    throw new UnsupportedOperationException();
  }

}
