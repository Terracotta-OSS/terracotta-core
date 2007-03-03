/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.object.field.TCField;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;
import com.tc.object.tx.optimistic.TCObjectClone;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

public class PhysicalApplicator extends BaseApplicator {

  private final TCClass clazz;

  public PhysicalApplicator(TCClass clazz, DNAEncoding encoding) {
    super(encoding);
    this.clazz = clazz;
  }

  /**
   * return the key value pairs of field names to shared objects for this source. We already updated the literals and
   * set the new TCObjectClone
   */
  public Map connectedCopy(Object source, Object dest, Map visited, ClientObjectManager objectManager,
                           OptimisticTransactionManager txManager) {
    Map map = new HashMap();
    Map cloned = new IdentityHashMap();

    TransparentAccess ta = (TransparentAccess) source;
    TransparentAccess da = (TransparentAccess) dest;

    Manageable sourceManageable = (Manageable) ta;
    Manageable destManaged = (Manageable) da;
    ta.__tc_getallfields(map);
    for (Iterator i = map.keySet().iterator(); i.hasNext();) {
      String k = (String) i.next();
      Object v = map.get(k);

      Object copyValue = createCopyIfNecessary(objectManager, visited, cloned, v);
      da.__tc_setfield(k, copyValue);
    }

    destManaged.__tc_managed(new TCObjectClone(sourceManageable.__tc_managed(), txManager));
    return cloned;
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    if (!(pojo instanceof TransparentAccess)) return addTo;

    Map map = new HashMap();
    TransparentAccess ta = (TransparentAccess) pojo;
    ta.__tc_getallfields(map);

    TCField[] fields = clazz.getPortableFields();
    if (clazz.isNonStaticInner()) {
      addTo.addNamedReference(clazz.getName(), clazz.getParentFieldName(), map.get(clazz.getParentFieldName()));
    }
    for (int i = 0; i < fields.length; i++) {
      Object o = map.get(fields[i].getName());

      if (o != null && isPortableReference(o.getClass())) {
        addTo.addNamedReference(fields[i].getName(), o);
      }
    }
    return addTo;
  }

  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object po) throws IOException,
      ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    String fieldName;
    Object fieldValue;

    while (cursor.next(encoding)) {
      PhysicalAction a = cursor.getPhysicalAction();
      Assert.eval(a.isTruePhysical());
      fieldName = a.getFieldName();
      fieldValue = a.getObject();

      tcObject.setValue(fieldName, fieldValue);
    }
  }

  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    if (!objectManager.isPortableInstance(pojo)) { return; }

    TCClass tcc = clazz;
    TCField[] fields;

    Map fieldValues = null;

    while (tcc != null) {
      fields = tcc.getPortableFields();

      if (fields.length > 0 && fieldValues == null) {
        fieldValues = new HashMap();
        if (pojo instanceof TransparentAccess) {
          // only need to do this once. The generated method takes care of walking up the class hierarchy
          ((TransparentAccess) pojo).__tc_getallfields(fieldValues);
        } else {
          throw new AssertionError("wrong type: " + pojo.getClass());
        }
      }

      for (int i = 0; i < fields.length; i++) {
        String fieldName = fields[i].getName();
        Object fieldValue = fieldValues.get(fieldName);

        if (fieldValue == null) {
          if (!fieldValues.containsKey(fieldName)) { throw new AssertionError(
                                                                              fieldName
                                                                                  + " does not exist in map returned from __tc_getallfields. Class is "
                                                                                  + tcc + ". field Values = "
                                                                                  + fieldValues); }
        }

        if (!objectManager.isPortableInstance(fieldValue)) {
          continue;
        }

        Object value = getDehydratableObject(fieldValue, objectManager);
        if (value == null) {
          // instead of ignoring non-portable objects we send ObjectID.NULL_ID so that the state can
          // be created at the server correctly if needed. Another reason is to null the reference across the cluster if
          // such an attempt was made.
          value = ObjectID.NULL_ID;
        }
        writer.addPhysicalAction(fieldName, value, fields[i].canBeReference());
      }
      tcc = tcc.getSuperclass();
    }

    if (clazz.isNonStaticInner() && fieldValues != null) {
      Object parentObject = fieldValues.get(clazz.getParentFieldName());
      Assert.assertNotNull("parentObject", parentObject);
      Object parentObjectID = getDehydratableObject(parentObject, objectManager);
      if (parentObjectID != null) {
        writer.setParentObjectID((ObjectID) parentObjectID);
      }
    }

  }

  public Object getNewInstance(ClientObjectManager objectManager, DNA dna) {
    throw new UnsupportedOperationException();
  }
}
