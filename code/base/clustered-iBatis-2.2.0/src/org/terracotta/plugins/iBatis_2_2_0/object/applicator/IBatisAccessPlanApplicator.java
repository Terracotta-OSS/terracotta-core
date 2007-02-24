/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.plugins.iBatis_2_2_0.object.applicator;

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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author Antonio Si
 */
public class IBatisAccessPlanApplicator extends BaseApplicator {
  private static final String CLASSNAME_FIELD_NAME  = "com.ibatis.sqlmap.engine.accessplan.BaseAccessPlan.className";
  private static final String CLAZZ_FIELD_NAME  = "com.ibatis.sqlmap.engine.accessplan.BaseAccessPlan.clazz";
  private static final String PROPERTY_NAMES_FIELD_NAME  = "com.ibatis.sqlmap.engine.accessplan.BaseAccessPlan.clazz.propertyNames";

  public IBatisAccessPlanApplicator(DNAEncoding encoding) {
    super(encoding);
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    addTo.addAnonymousReference(getClazz(pojo));
    addTo.addAnonymousReference(getPropertyNames(pojo));
    return addTo;
  }

  private Object getClazz(Object pojo) {
    //return get(pojo, "getGetters", new Class[] {}, new Object[] {});
    return getField(pojo, "clazz");
  }

  private Object getPropertyNames(Object pojo) {
    //return get(pojo, "getGetters", new Class[] {}, new Object[] {});
    return getField(pojo, "propertyNames");
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

  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object po) throws IOException,
      IllegalArgumentException, ClassNotFoundException {
    // TODO
  }

  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    String className = pojo.getClass().getName();
    writer.addPhysicalAction(CLASSNAME_FIELD_NAME, className);
    
    Class clazz = (Class)getClazz(pojo);
    writer.addPhysicalAction(CLAZZ_FIELD_NAME, clazz);
    
    Object propertyNames = getPropertyNames(pojo);
    Object dehydratablePropertyNames = getDehydratableObject(propertyNames, objectManager);
    writer.addPhysicalAction(PROPERTY_NAMES_FIELD_NAME, dehydratablePropertyNames);
  }

  public Object getNewInstance(ClientObjectManager objectManager, DNA dna) throws IOException, ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    Assert.assertEquals(3, cursor.getActionCount());
    
    cursor.next(encoding);
    PhysicalAction a = cursor.getPhysicalAction();
    String className = (String)a.getObject();

    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    Class target = (Class) a.getObject();

    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    Object propertyNames = a.getObject();
    propertyNames = objectManager.lookupObject((ObjectID) propertyNames);
    
    return create(target, className, propertyNames);
  }

  private Object create(Class target, String className, Object propertyNames) {
    try {
      Class clazz = target.getClassLoader().loadClass(className);
      
      Constructor c = clazz.getDeclaredConstructor(new Class[]{Class.class, String[].class});
      c.setAccessible(true);
      Object o = c.newInstance(new Object[]{target, propertyNames});
      
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
    } catch (InstantiationException e) {
      throw new TCRuntimeException(e);
    }
  }

  public Map connectedCopy(Object source, Object dest, Map visited, ClientObjectManager objectManager,
                           OptimisticTransactionManager txManager) {
    Map cloned = new IdentityHashMap();

    // TODO

    return cloned;
  }
}
