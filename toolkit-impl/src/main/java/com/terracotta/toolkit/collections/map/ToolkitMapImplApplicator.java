/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import com.tc.abortable.AbortedOperationException;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.applicator.BaseApplicator;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.terracotta.toolkit.object.DestroyApplicator;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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
      final int method = action.getMethod();
      final Object[] params = action.getParameters();
      apply(objectManager, po, method, params);
    }
  }

  protected void apply(final ClientObjectManager objectManager, final Object po, final int method, final Object[] params)
      throws ClassNotFoundException {
    final ToolkitMapImpl m = (ToolkitMapImpl) po;
    switch (method) {
      case SerializationUtil.PUT:
        final Object k = params[0];
        final Object v = params[1];
        final Object pkey = getObjectForKey(objectManager, k);
        final Object value = getObjectForValue(objectManager, v);

        m.internalPut(pkey, value);
        break;
      case SerializationUtil.REMOVE:
        Object rkey;
        try {
          rkey = params[0] instanceof ObjectID ? objectManager.lookupObject((ObjectID) params[0]) : params[0];
        } catch (AbortedOperationException e) {
          throw new TCRuntimeException(e);
        }
        m.internalRemove(rkey);

        break;
      case SerializationUtil.CLEAR:
        m.internalClear();
        break;
      case SerializationUtil.DESTROY:
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

    final Map map = (Map) pojo;
    for (final Iterator i = map.entrySet().iterator(); i.hasNext();) {
      final Entry entry = (Entry) i.next();
      final Object key = entry.getKey();
      final Object value = entry.getValue();

      if (!objectManager.isPortableInstance(key)) {
        continue;
      }
      if (!objectManager.isPortableInstance(value)) {
        continue;
      }

      final Object addKey = getDehydratableObject(key, objectManager);
      final Object addValue = getDehydratableObject(value, objectManager);

      if (addKey == null || addValue == null) {
        continue;
      }

      writer.addLogicalAction(SerializationUtil.PUT, new Object[] { addKey, addValue });
    }
  }

  @Override
  public Object getNewInstance(final ClientObjectManager objectManager, final DNA dna) throws IOException,
      ClassNotFoundException {
    if (false) { throw new IOException(); } // silence compiler warning
    if (false) { throw new ClassNotFoundException(); } // silence compiler warning
    throw new UnsupportedOperationException();
  }
}
