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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class AccessibleObjectApplicator extends BaseApplicator {
  private static final String DECLARING_CLASS_FIELD_NAME        = "java.lang.reflect.AccessibleObject.declaringClass";
  private static final String ACCESSIBLE_OBJECT_NAME_FILED_NAME = "java.lang.reflect.AccessibleObject.name";
  private static final String OVERRIDE_FILED_NAME               = "java.lang.reflect.AccessibleObject.override";
  private static final String PARAMETER_TYPES                   = "java.lang.reflect.AccessibleObject.parameterTypes";
  private static final String ACCESSIBLE_OBJECT_TYPE            = "java.lang.reflect.AccessibleObject.type";
  private static final String METHOD_CLASS_NAME                 = Method.class.getName();
  private static final String CONSTRUCTOR_CLASS_NAME            = Constructor.class.getName();
  private static final String FIELD_CLASS_NAME                  = Field.class.getName();

  public AccessibleObjectApplicator(DNAEncoding encoding) {
    super(encoding);
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    if (pojo instanceof Method) {
      Method m = (Method) pojo;
      addTo.addAnonymousReference(m.getDeclaringClass());
      addTo.addAnonymousReference(m.getName());
      addTo.addAnonymousReference(m.getParameterTypes());
    } else if (pojo instanceof Constructor) {
      Constructor c = (Constructor) pojo;
      addTo.addAnonymousReference(c.getDeclaringClass());
      addTo.addAnonymousReference(c.getName());
      addTo.addAnonymousReference(c.getParameterTypes());
    } else if (pojo instanceof Field) {
      Field f = (Field) pojo;
      addTo.addAnonymousReference(f.getDeclaringClass());
      addTo.addAnonymousReference(f.getName());
    }
    return addTo;
  }

  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object po) throws IOException,
      IllegalArgumentException, ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    Assert.eval(cursor.getActionCount() <= 1);

    if (cursor.next(encoding)) {
      PhysicalAction a = (PhysicalAction) cursor.getAction();
      Boolean value = (Boolean) a.getObject();
      ((AccessibleObject) po).setAccessible(value.booleanValue());
    }
  }

  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    Class declaringClass = null;
    String name = null;
    Class[] parameterTypes = null;
    AccessibleObject ao = (AccessibleObject) pojo;
    boolean override = ao.isAccessible();
    if (ao instanceof Method) {
      Method m = (Method) ao;
      declaringClass = m.getDeclaringClass();
      name = m.getName();
      parameterTypes = m.getParameterTypes();
    } else if (ao instanceof Constructor) {
      Constructor c = (Constructor) ao;
      declaringClass = c.getDeclaringClass();
      name = c.getName();
      parameterTypes = c.getParameterTypes();
    } else if (ao instanceof Field) {
      Field f = (Field) ao;
      declaringClass = f.getDeclaringClass();
      name = f.getName();
    }
    writer.addPhysicalAction(ACCESSIBLE_OBJECT_TYPE, ao.getClass().getName());
    writer.addPhysicalAction(DECLARING_CLASS_FIELD_NAME, declaringClass);
    writer.addPhysicalAction(ACCESSIBLE_OBJECT_NAME_FILED_NAME, name);
    writer.addPhysicalAction(OVERRIDE_FILED_NAME, new Boolean(override));
    if (!(ao instanceof Field)) {
      writer.addPhysicalAction(PARAMETER_TYPES, parameterTypes);
    }
  }

  public Object getNewInstance(ClientObjectManager objectManager, DNA dna) throws IOException, ClassNotFoundException {
    Class[] parameterTypes = null;
    
    DNACursor cursor = dna.getCursor();

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

    if (!FIELD_CLASS_NAME.equals(objectType)) {
      cursor.next(encoding);
      a = cursor.getPhysicalAction();
      Object[] values = (Object[]) a.getObject();
      parameterTypes = new Class[values.length];
      System.arraycopy(values, 0, parameterTypes, 0, values.length);
    }

    if (METHOD_CLASS_NAME.equals(objectType)) {
      try {
        Method m = declaringClass.getDeclaredMethod(name, parameterTypes);
        m.setAccessible(override.booleanValue());
        return m;
      } catch (NoSuchMethodException e) {
        throw new AssertionError(e);
      }
    } else if (CONSTRUCTOR_CLASS_NAME.equals(objectType)) {
      try {
        Constructor c = declaringClass.getDeclaredConstructor(parameterTypes);
        c.setAccessible(override.booleanValue());
        return c;
      } catch (NoSuchMethodException e) {
        throw new AssertionError(e);
      }
    } else if (FIELD_CLASS_NAME.equals(objectType)) {
      try {
        Field f = declaringClass.getDeclaredField(name);
        f.setAccessible(override.booleanValue());
        return f;
      } catch (SecurityException e) {
        throw new AssertionError(e);
      } catch (NoSuchFieldException e) {
        throw new AssertionError(e);
      }
    } else {
      throw new AssertionError("Object type not known.");
    }
  }

  public Map connectedCopy(Object source, Object dest, Map visited, ClientObjectManager objectManager,
                           OptimisticTransactionManager txManager) {
    throw new TCNotSupportedMethodException();
  }
}
