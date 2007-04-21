/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.exception.TCNotSupportedMethodException;
import com.tc.object.ClientObjectManager;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;
import com.tc.util.Assert;

import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.Map;

public class AccessibleObjectApplicator extends BaseApplicator {
  private static final String DECLARING_CLASS_FIELD_NAME        = "java.lang.reflect.AccessibleObject.declaringClass";
  private static final String ACCESSIBLE_OBJECT_NAME_FILED_NAME = "java.lang.reflect.AccessibleObject.name";
  private static final String OVERRIDE_FILED_NAME               = "java.lang.reflect.AccessibleObject.override";
  private static final String PARAMETER_TYPES                   = "java.lang.reflect.AccessibleObject.parameterTypes";
  private static final String ACCESSIBLE_OBJECT_TYPE            = "java.lang.reflect.AccessibleObject.type";
  private static final String METHOD_CLASS_NAME                 = Method.class.getName();

  public AccessibleObjectApplicator(DNAEncoding encoding) {
    super(encoding);
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    if (pojo instanceof Method) {
      addTo.addAnonymousReference(((Method) pojo).getDeclaringClass());
      addTo.addAnonymousReference(((Method) pojo).getName());
      addTo.addAnonymousReference(((Method) pojo).getParameterTypes());
    }
    return addTo;
  }

  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object po) throws IOException,
      IllegalArgumentException, ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    Assert.eval(cursor.getActionCount() <= 1);

    if (po instanceof Method) {
      if (cursor.next(encoding)) {
        PhysicalAction a = (PhysicalAction) cursor.getAction();
        Boolean value = (Boolean) a.getObject();
        ((Method)po).setAccessible(value.booleanValue());
      }
    }
  }

  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    Class declaringClass = null;
    String name = null;
    boolean override = false;
    AccessibleObject ao = (AccessibleObject) pojo;
    if (ao instanceof Method) {
      declaringClass = ((Method) ao).getDeclaringClass();
      name = ((Method) ao).getName();
      override = ((Method) ao).isAccessible();

      Class[] parameterTypes = ((Method) ao).getParameterTypes();

      writer.addPhysicalAction(ACCESSIBLE_OBJECT_TYPE, pojo.getClass().getName());
      writer.addPhysicalAction(DECLARING_CLASS_FIELD_NAME, declaringClass);
      writer.addPhysicalAction(ACCESSIBLE_OBJECT_NAME_FILED_NAME, name);
      writer.addPhysicalAction(OVERRIDE_FILED_NAME, new Boolean(override));
      writer.addPhysicalAction(PARAMETER_TYPES, parameterTypes);
    }
  }

  public Object getNewInstance(ClientObjectManager objectManager, DNA dna) throws IOException, ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    Assert.assertEquals(5, cursor.getActionCount());

    cursor.next(encoding);
    PhysicalAction a = cursor.getPhysicalAction();
    String objectType = (String) a.getObject();

    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    Class declaringClass = (Class) a.getObject();

    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    String name = (String) a.getObject();

    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    Boolean override = (Boolean) a.getObject();

    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    Object[] values = (Object[]) a.getObject();
    Class[] parameterTypes = new Class[values.length];
    System.arraycopy(values, 0, parameterTypes, 0, values.length);

    if (METHOD_CLASS_NAME.equals(objectType)) {
      try {
        Method m = declaringClass.getDeclaredMethod(name, parameterTypes);
        m.setAccessible(override.booleanValue());
        return m;
      } catch (NoSuchMethodException e) {
        throw new AssertionError(e);
      }
    }
    throw new AssertionError("Object type not known.");
  }

  public Map connectedCopy(Object source, Object dest, Map visited, ClientObjectManager objectManager,
                           OptimisticTransactionManager txManager) {
    throw new TCNotSupportedMethodException();
  }
}
