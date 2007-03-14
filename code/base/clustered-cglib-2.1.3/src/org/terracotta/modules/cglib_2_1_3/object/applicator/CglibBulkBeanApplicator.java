/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.cglib_2_1_3.object.applicator;

import com.tc.exception.TCNotSupportedMethodException;
import com.tc.exception.TCRuntimeException;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.applicator.BaseApplicator;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;
import com.tc.util.Assert;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class CglibBulkBeanApplicator extends BaseApplicator {
  private static final String GETTERS_FIELD_NAME  = "net.sf.cglib.beans.BulkBean.getters";
  private static final String SETTERS_FIELD_NAME  = "net.sf.cglib.beans.BulkBean.setters";
  private static final String TYPES_FIELD_NAME  = "net.sf.cglib.beans.BulkBean.types";
  private static final String TARGET_FIELD_NAME  = "net.sf.cglib.beans.BulkBean.target";

  public CglibBulkBeanApplicator(DNAEncoding encoding) {
    super(encoding);
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    addTo.addAnonymousReference(getGetters(pojo));
    addTo.addAnonymousReference(getSetters(pojo));
    addTo.addAnonymousReference(getPropertyTypes(pojo));
    addTo.addAnonymousReference(getTarget(pojo));
    return addTo;
  }

  private Object getGetters(Object pojo) {
    //return get(pojo, "getGetters", new Class[] {}, new Object[] {});
    return getField(pojo, "getters");
  }

  private Object getSetters(Object pojo) {
    //return get(pojo, "getGetters", new Class[] {}, new Object[] {});
    return getField(pojo, "setters");
  }

  private Object getPropertyTypes(Object pojo) {
    //return get(pojo, "getPropertyTypes", new Class[] {}, new Object[] {});
    return getField(pojo, "types");
  }
  
  private Class getTarget(Object pojo) {
    return (Class)getField(pojo, "target");
  }

  private Object getField(Object pojo, String fieldName) {
    try {
      Field field = pojo.getClass().getSuperclass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(pojo);
    } catch (IllegalArgumentException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new TCRuntimeException(e);
    }
  }

  private Object get(Object pojo, String methodName, Class[] parameterTypes, Object[] parameterValues) {
    try {
      Method m = pojo.getClass().getMethod(methodName, parameterTypes);
      Object callBack = m.invoke(pojo, parameterValues);
      return callBack;
    } catch (NoSuchMethodException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new TCRuntimeException(e);
    }
  }

  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object po) throws IOException,
      IllegalArgumentException, ClassNotFoundException {
    // 
  }

  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {

    Class target = getTarget(pojo);
    writer.addPhysicalAction(TARGET_FIELD_NAME, target);
    
    Object getters = getGetters(pojo);
    Object dehydratableGetters = getDehydratableObject(getters, objectManager);
    writer.addPhysicalAction(GETTERS_FIELD_NAME, dehydratableGetters);

    Object setters = getSetters(pojo);
    Object dehydratableSetters = getDehydratableObject(setters, objectManager);
    writer.addPhysicalAction(SETTERS_FIELD_NAME, dehydratableSetters);
    
    Object types = getPropertyTypes(pojo);
    Object dehydratableTypes = getDehydratableObject(types, objectManager);
    writer.addPhysicalAction(TYPES_FIELD_NAME, dehydratableTypes);
  }

  public Object getNewInstance(ClientObjectManager objectManager, DNA dna) throws IOException, ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    Assert.assertEquals(4, cursor.getActionCount());

    cursor.next(encoding);
    PhysicalAction a = cursor.getPhysicalAction();
    Class target = (Class) a.getObject();

    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    Object getters = a.getObject();
    getters = objectManager.lookupObject((ObjectID) getters);
    
    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    Object setters = a.getObject();
    setters = objectManager.lookupObject((ObjectID) setters);
    
    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    Object types = a.getObject();
    types = objectManager.lookupObject((ObjectID) types);

    return create(target, getters, setters, types);
  }

  private Object create(Class target, Object getters, Object setters, Object types) {
    try {
      Class bulkBeanClass = target.getClassLoader().loadClass("net.sf.cglib.beans.BulkBean");
      
      Method m = bulkBeanClass.getDeclaredMethod("create", new Class[] { Class.class, String[].class, String[].class, Class[].class});
      Object o = m.invoke(null, new Object[] { target, getters, setters, types });
      return o;
    } catch (NoSuchMethodException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new TCRuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new TCRuntimeException(e);
    }
  }

  public Map connectedCopy(Object source, Object dest, Map visited, ClientObjectManager objectManager,
                           OptimisticTransactionManager txManager) {
    throw new TCNotSupportedMethodException();
  }
}
