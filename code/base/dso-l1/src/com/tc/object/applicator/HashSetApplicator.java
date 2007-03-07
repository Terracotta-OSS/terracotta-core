/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.bytecode.Manageable;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;
import com.tc.object.tx.optimistic.TCObjectClone;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ChangeApplicator for HashSets.
 */
public class HashSetApplicator extends BaseApplicator {

  public HashSetApplicator(DNAEncoding encoding) {
    super(encoding);
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

  protected void apply(ClientObjectManager objectManager, Set set, int method, Object[] params) throws ClassNotFoundException {
    switch (method) {
      case SerializationUtil.ADD:
        Object v = getValue(params);
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

    for (int i = 0; i < params.length; i++) {
      retParams.add(params[i] instanceof ObjectID ? objectManager.lookupObject((ObjectID) params[i]) : params[i]);
    }
    return retParams;
  }

  private Object getValue(Object[] params) {
    // hack to deal with trove set which replaces on set unlike java Set which does not
    return params.length == 2 ? params[1] : params[0];
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

  public Map connectedCopy(Object source, Object dest, Map visited, ClientObjectManager objectManager,
                           OptimisticTransactionManager txManager) {
    Map cloned = new IdentityHashMap();

    Manageable sourceManageable = (Manageable) source;
    Manageable destManaged = (Manageable) dest;

    Set sourceSet = (Set) source;
    Set destSet = (Set) dest;

    for (Iterator i = sourceSet.iterator(); i.hasNext();) {
      Object v = i.next();
      Object copyValue = null;

      if (isLiteralInstance(v)) {
        copyValue = v;
      } else if (visited.containsKey(v)) {
        Assert.eval(visited.get(v) != null);
        copyValue = visited.get(v);
      } else {
        Assert.eval(!isLiteralInstance(v));
        copyValue = objectManager.createNewCopyInstance(v, createParentIfNecessary(visited, objectManager, cloned, v));

        visited.put(v, copyValue);
        cloned.put(v, copyValue);
      }
      destSet.add(copyValue);
    }

    destManaged.__tc_managed(new TCObjectClone(sourceManageable.__tc_managed(), txManager));
    return cloned;
  }
}
