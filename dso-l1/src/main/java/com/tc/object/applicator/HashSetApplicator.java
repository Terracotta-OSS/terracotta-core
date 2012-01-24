/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.logging.TCLogger;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * ChangeApplicator for HashSets.
 */
public class HashSetApplicator extends BaseApplicator {

  public HashSetApplicator(DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }

  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object pojo) throws IOException,
      ClassNotFoundException {
    Set set = (Set) pojo;
    DNACursor cursor = dna.getCursor();

    while (cursor.next(encoding)) {
      LogicalAction action = cursor.getLogicalAction();
      int method = action.getMethod();
      Object[] params = action.getParameters();
      apply(objectManager, set, method, params);
    }
  }

  protected void apply(ClientObjectManager objectManager, Set set, int method, Object[] params)
      throws ClassNotFoundException {
    switch (method) {
      case SerializationUtil.ADD:
        Object v = params[0];
        Object value = v instanceof ObjectID ? objectManager.lookupObject((ObjectID) v) : v;
        set.add(value);
        break;
      case SerializationUtil.REMOVE:
        Object rkey = params[0] instanceof ObjectID ? objectManager.lookupObject((ObjectID) params[0]) : params[0];
        set.remove(rkey);
        break;
      case SerializationUtil.REMOVE_ALL:
        set.removeAll(getObjectParams(objectManager, params));
        break;
      case SerializationUtil.CLEAR:
        set.clear();
        break;
      default:
        throw new AssertionError("invalid action:" + method);
    }
  }

  private List getObjectParams(ClientObjectManager objectManager, Object[] params) throws ClassNotFoundException {
    List retParams = new ArrayList(params.length);

    for (Object param : params) {
      retParams.add(param instanceof ObjectID ? objectManager.lookupObject((ObjectID) param) : param);
    }
    return retParams;
  }

  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    Set set = (Set) pojo;
    for (Iterator i = set.iterator(); i.hasNext();) {
      Object value = i.next();
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

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    Set set = (Set) pojo;
    for (Iterator i = set.iterator(); i.hasNext();) {
      Object o = i.next();
      if (o != null && isPortableReference(o.getClass())) {
        addTo.addAnonymousReference(o);
      }
    }
    return addTo;
  }

  public Object getNewInstance(ClientObjectManager objectManager, DNA dna) {
    throw new UnsupportedOperationException();
  }

}
