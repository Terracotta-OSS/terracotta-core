/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObjectExternal;
import com.tc.object.TraversedReferences;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.field.TCField;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class PhysicalApplicator extends BaseApplicator {

  private final TCClass clazz;

  public PhysicalApplicator(TCClass clazz, DNAEncoding encoding) {
    super(encoding, TCLogging.getLogger(PhysicalApplicator.class));
    this.clazz = clazz;
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    if (!(pojo instanceof TransparentAccess)) return addTo;

    Map map = new HashMap();
    getAllFields(pojo, map);

    TCField[] fields = clazz.getPortableFields();
    if (clazz.isNonStaticInner()) {
      String qualifiedParentFieldName = clazz.getParentFieldName();
      final String fName;
      try {
        fName = ClassUtils.parseFullyQualifiedFieldName(qualifiedParentFieldName).getShortFieldName();
      } catch (ParseException e) {
        throw new AssertionError(e);
      }

      addTo.addNamedReference(clazz.getName(), fName, map.get(qualifiedParentFieldName));
    }
    for (TCField field : fields) {
      Object o = map.get(field.getName());

      if (o != null && isPortableReference(o.getClass())) {
        addTo.addNamedReference(field.getName(), o);
      }
    }
    return addTo;
  }

  public void hydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNA dna, Object po)
      throws IOException, ClassNotFoundException {
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

  public void dehydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNAWriter writer, Object pojo) {
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
          getAllFields(pojo, fieldValues);

          // The writing of the parentID must happen before any physical actions
          if (clazz.isNonStaticInner()) {
            Object parentObject = fieldValues.get(clazz.getParentFieldName());
            Assert.assertNotNull("parentObject", parentObject);
            Object parentObjectID = getDehydratableObject(parentObject, objectManager);
            if (parentObjectID != null) {
              writer.setParentObjectID((ObjectID) parentObjectID);
            }
          }
        } else {
          throw new AssertionError("wrong type: " + pojo.getClass());
        }
      }

      for (TCField field : fields) {
        String fieldName = field.getName();
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
        writer.addPhysicalAction(fieldName, value, field.canBeReference());
      }
      tcc = tcc.getSuperclass();
    }

  }

  public Object getNewInstance(ApplicatorObjectManager objectManager, DNA dna) {
    throw new UnsupportedOperationException();
  }

  private static void getAllFields(Object o, Map dest) {
    // This is ugly, but doing the getStackTrace stuff inside of __tc_getallfields() seems
    // to trip a hotspot bug (DEV-67)
    if (o instanceof Throwable) {
      // This has the side effect of getting the stack trace initilized in the target object
      ((Throwable) o).getStackTrace();
    }
    ((TransparentAccess) o).__tc_getallfields(dest);
  }

}
