/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import com.tc.abortable.AbortedOperationException;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ClientObjectManager;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.applicator.BaseApplicator;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.platform.PlatformService;

import java.io.IOException;
import java.util.List;

public class ToolkitListImplApplicator extends BaseApplicator {
  private static final TCLogger LOGGER = TCLogging.getLogger(ToolkitListImplApplicator.class);
  
  public ToolkitListImplApplicator(DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }
  
  @Override
  public Object getNewInstance(ClientObjectManager objectManager, DNA dna, PlatformService platformService) {
    return new ToolkitListImpl(platformService);
  }

  @Override
  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object po) throws IOException,
      ClassNotFoundException {
    List list = (List) po;
    DNACursor cursor = dna.getCursor();

    while (cursor.next(encoding)) {
      LogicalAction action = cursor.getLogicalAction();
      LogicalOperation method = action.getLogicalOperation();
      Object[] params = action.getParameters();

      for (int i = 0, n = params.length; i < n; i++) {
        Object param = params[i];
        if (param instanceof ObjectID) {
          try {
            params[i] = objectManager.lookupObject((ObjectID) param);
          } catch (AbortedOperationException e) {
             throw new TCRuntimeException(e);
          }
        }
      }

      try {
        apply(list, method, params);
      } catch (IndexOutOfBoundsException ioobe) {
        LOGGER.error("Error applying update to " + po, ioobe);
      }
    }
  }

  private void apply(List list, LogicalOperation method, Object[] params) {
    ToolkitListImpl internalList = (ToolkitListImpl) list;
    final int size = internalList.size();
    switch (method) {
      case ADD:
        internalList.internalAdd(params[0]);
        break;
      case ADD_AT:
        int aaindex = ((Integer) params[0]).intValue();
        if (aaindex > size) {
          getLogger().error("Inserting at index " + size + " instead of requested index " + aaindex
                                + "because list is only of size " + size);
          aaindex = size;
        }
        internalList.internalAdd(aaindex, params[1]);
        break;
      case SET:
        int sindex = ((Integer) params[0]).intValue();
        if (sindex >= size) {
          getLogger().error("Cannot set element at index " + sindex + " because object is only of size " + size);
          return;
        }
        internalList.internalSet(sindex, params[1]);
        break;
      case REMOVE:
        internalList.internalRemove(params[0]);
        break;
      case REMOVE_AT:
        int raindex = ((Integer) params[0]).intValue();
        if (raindex >= size) {
          getLogger().error("Cannot remove element at index " + raindex + " because object is only of size " + size);
          return;
        }
        internalList.internalRemove(raindex);
        break;
      case REMOVE_RANGE:
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
      case CLEAR:
        internalList.internalClear();
        break;
      case DESTROY:
        internalList.applyDestroy();
        break;
      default:
        throw new AssertionError("invalid action:" + method);
    }
  }

  @Override
  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    // Nothing to dehydrate
  }

  @Override
  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    return addTo;
  }
}
