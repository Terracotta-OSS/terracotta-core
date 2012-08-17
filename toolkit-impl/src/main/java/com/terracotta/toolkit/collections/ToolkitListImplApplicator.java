/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import com.tc.logging.TCLogger;
import com.tc.object.SerializationUtil;
import com.tc.object.TraversedReferences;
import com.tc.object.applicator.ListApplicator;
import com.tc.object.dna.api.DNAEncoding;

import java.util.List;

public class ToolkitListImplApplicator extends ListApplicator {
  public ToolkitListImplApplicator(DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }

  @Override
  protected void apply(List list, int method, Object[] params) {
    ToolkitListImpl internalList = (ToolkitListImpl) list;
    final int size = internalList.size();
    switch (method) {
      case SerializationUtil.ADD:
      case SerializationUtil.ADD_LAST:
        internalList.internalAdd(params[0]);
        break;
      case SerializationUtil.INSERT_AT:
      case SerializationUtil.ADD_AT:
        int aaindex = ((Integer) params[0]).intValue();
        if (aaindex > size) {
          getLogger().error("Inserting at index " + size + " instead of requested index " + aaindex
                                + "because list is only of size " + size);
          aaindex = size;
        }
        internalList.internalAdd(aaindex, params[1]);
        break;
      case SerializationUtil.ADD_FIRST:
        internalList.internalAdd(0, params[0]);
        break;
      case SerializationUtil.SET_ELEMENT:
      case SerializationUtil.SET:
        int sindex = ((Integer) params[0]).intValue();
        if (sindex >= size) {
          getLogger().error("Cannot set element at index " + sindex + " because object is only of size " + size);
          return;
        }
        internalList.internalSet(sindex, params[1]);
        break;
      case SerializationUtil.REMOVE:
        internalList.internalRemove(params[0]);
        break;
      case SerializationUtil.REMOVE_AT:
        int raindex = ((Integer) params[0]).intValue();
        if (raindex >= size) {
          getLogger().error("Cannot remove element at index " + raindex + " because object is only of size " + size);
          return;
        }
        internalList.internalRemove(raindex);
        break;
      case SerializationUtil.REMOVE_RANGE:
        int fromIndex = ((Integer) params[0]).intValue();
        if (fromIndex >= size) {
          getLogger().error("Cannot remove element at index " + fromIndex + " because object is only of size " + size);
          return;
        }
        int toIndex = ((Integer) params[1]).intValue();
        if (toIndex > size) {
          getLogger().error("Cannot remove element at index " + (toIndex - 1) + " because object is only of size "
                                + size);
          return;
        }
        int removeIndex = fromIndex;
        while (fromIndex++ < toIndex) {
          internalList.internalRemove(removeIndex);
        }
        break;
      case SerializationUtil.REMOVE_FIRST:
        if (size > 0) {
          internalList.internalRemove(0);
        } else {
          getLogger().error("Cannot removeFirst() because list is empty");
        }
        break;
      case SerializationUtil.REMOVE_LAST:
        if (size > 0) {
          internalList.internalRemove(internalList.size() - 1);
        } else {
          getLogger().error("Cannot removeLast() because list is empty");
        }
        break;
      case SerializationUtil.REMOVE_ALL:
        for (Object o : params) {
          internalList.internalRemove(o);
        }
        break;
      case SerializationUtil.CLEAR:
        internalList.internalClear();
        break;
      case SerializationUtil.DESTROY:
        internalList.applyDestroy();
        break;
      default:
        throw new AssertionError("invalid action:" + method);
    }
  }

  @Override
  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    return addTo;
  }

}
