/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import com.tc.abortable.AbortedOperationException;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
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
import com.terracotta.toolkit.object.DestroyApplicator;

import java.io.IOException;

public class ToolkitMapImplApplicator extends BaseApplicator {
  public ToolkitMapImplApplicator(final DNAEncoding encoding, final TCLogger logger) {
    super(encoding, logger);
  }

  @Override
  public TraversedReferences getPortableObjects(final Object pojo, final TraversedReferences addTo) {
    return addTo;
  }

  @Override
  public void hydrate(final ClientObjectManager objectManager, final TCObject tco, final DNA dna, final Object po)
      throws IOException, ClassNotFoundException {
    final DNACursor cursor = dna.getCursor();

    while (cursor.next(this.encoding)) {
      final LogicalAction action = cursor.getLogicalAction();
      final LogicalOperation method = action.getLogicalOperation();
      final Object[] params = action.getParameters();
      apply(objectManager, po, method, params);
    }
  }

  protected void apply(final ClientObjectManager objectManager, final Object po, final LogicalOperation method, final Object[] params)
      throws ClassNotFoundException {
    final ToolkitMapImpl m = (ToolkitMapImpl) po;
    switch (method) {
      case PUT:
        final Object k = params[0];
        final Object v = params[1];
        final Object pkey = getObjectForKey(objectManager, k);
        final Object value = getObjectForValue(objectManager, v);

        m.internalPut(pkey, value);
        break;
      case REMOVE:
        Object rkey;
        try {
          rkey = params[0] instanceof ObjectID ? objectManager.lookupObject((ObjectID) params[0]) : params[0];
        } catch (AbortedOperationException e) {
          throw new TCRuntimeException(e);
        }
        m.internalRemove(rkey);

        break;
      case CLEAR:
        m.internalClear();
        break;
      case DESTROY:
        ((DestroyApplicator) m).applyDestroy();
        break;

      default:
        throw new AssertionError("invalid action:" + method);
    }
  }

  // This can be overridden by subclass if you want different behavior.
  private Object getObjectForValue(final ClientObjectManager objectManager, final Object v)
      throws ClassNotFoundException {
    try {
      return (v instanceof ObjectID ? objectManager.lookupObject((ObjectID) v) : v);
    } catch (AbortedOperationException e) {
      throw new TCRuntimeException(e);
    }
  }

  // This can be overridden by subclass if you want different behavior.
  protected Object getObjectForKey(final ClientObjectManager objectManager, final Object k)
      throws ClassNotFoundException {
    try {
      return (k instanceof ObjectID ? objectManager.lookupObject((ObjectID) k) : k);
    } catch (AbortedOperationException e) {
      throw new TCRuntimeException(e);
    }
  }

  @Override
  public void dehydrate(final ClientObjectManager objectManager, final TCObject tco, final DNAWriter writer,
                        final Object pojo) {
    // Nothing to dehydrate
  }

  @Override
  public Object getNewInstance(final ClientObjectManager objectManager, final DNA dna, PlatformService platformService) {
    return new ToolkitMapImpl(platformService);
  }
}
