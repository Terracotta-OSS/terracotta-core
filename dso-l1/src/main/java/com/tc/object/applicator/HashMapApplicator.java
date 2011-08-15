/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.logging.TCLogger;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObjectExternal;
import com.tc.object.TraversedReferences;
import com.tc.object.bytecode.TCMap;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Apply a logical action to an object
 */
public class HashMapApplicator extends BaseApplicator {

  public HashMapApplicator(final DNAEncoding encoding, final TCLogger logger) {
    super(encoding, logger);
  }

  public TraversedReferences getPortableObjects(final Object pojo, final TraversedReferences addTo) {
    final Map m = (Map) pojo;
    filterPortableObjects(m.keySet(), addTo);
    filterPortableObjects(m.values(), addTo);
    return addTo;
  }

  private void filterPortableObjects(final Collection objects, final TraversedReferences addTo) {
    for (final Iterator i = objects.iterator(); i.hasNext();) {
      final Object o = i.next();
      if (o != null && isPortableReference(o.getClass())) {
        addTo.addAnonymousReference(o);
      }
    }
  }

  public void hydrate(final ApplicatorObjectManager objectManager, final TCObjectExternal TCObjectExternal,
                      final DNA dna, final Object po) throws IOException, ClassNotFoundException {
    final DNACursor cursor = dna.getCursor();

    while (cursor.next(this.encoding)) {
      final LogicalAction action = cursor.getLogicalAction();
      final int method = action.getMethod();
      final Object[] params = action.getParameters();
      apply(objectManager, po, method, params);
    }
  }

  protected void apply(final ApplicatorObjectManager objectManager, final Object po, final int method,
                       final Object[] params) throws ClassNotFoundException {
    final Map m = (Map) po;
    switch (method) {
      case SerializationUtil.PUT:
        final Object k = getKey(params);
        final Object v = getValue(params);
        final Object pkey = getObjectForKey(objectManager, k);

        final Object value = getObjectForValue(objectManager, v);

        if (m instanceof TCMap) {
          ((TCMap) m).__tc_applicator_put(pkey, value);
        } else {
          m.put(pkey, value);
        }

        break;
      case SerializationUtil.REMOVE:
        final Object rkey = params[0] instanceof ObjectID ? objectManager.lookupObject((ObjectID) params[0])
            : params[0];
        if (m instanceof TCMap) {
          ((TCMap) m).__tc_applicator_remove(rkey);
        } else {
          m.remove(rkey);
        }

        break;
      case SerializationUtil.CLEAR:
        if (m instanceof TCMap) {
          ((TCMap) m).__tc_applicator_clear();
        } else {
          m.clear();
        }
        break;
      default:
        throw new AssertionError("invalid action:" + method);
    }
  }

  // This can be overridden by subclass if you want different behavior.
  protected Object getObjectForValue(final ApplicatorObjectManager objectManager, final Object v)
      throws ClassNotFoundException {
    return (v instanceof ObjectID ? objectManager.lookupObject((ObjectID) v) : v);
  }

  // This can be overridden by subclass if you want different behavior.
  protected Object getObjectForKey(final ApplicatorObjectManager objectManager, final Object k)
      throws ClassNotFoundException {
    return (k instanceof ObjectID ? objectManager.lookupObject((ObjectID) k) : k);
  }

  private Object getValue(final Object[] params) {
    // Hack hack big hack for trove maps which replace the key on set as opposed to HashMaps which do not.
    return params.length == 3 ? params[2] : params[1];
  }

  private Object getKey(final Object[] params) {
    return params.length == 3 ? params[1] : params[0];
  }

  public void dehydrate(final ApplicatorObjectManager objectManager, final TCObjectExternal TCObjectExternal,
                        final DNAWriter writer, final Object pojo) {

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

  public Object getNewInstance(final ApplicatorObjectManager objectManager, final DNA dna) throws IOException,
      ClassNotFoundException {
    if (false) { throw new IOException(); } // silence compiler warning
    if (false) { throw new ClassNotFoundException(); } // silence compiler warning
    throw new UnsupportedOperationException();
  }
}
