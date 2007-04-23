/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.hibernate_3_1_2.object.applicator;

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
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author Antonio Si
 */
public class HibernateProxyApplicator extends BaseApplicator {
  private static final String LAZY_INITIALIZER_FIELD_NAME = "org.hibernate.proxy.HibernateProxy.lazyInitializer";
  private static final String PERSISTENT_CLASS_FIELD_NAME = "org.hibernate.proxy.HibernateProxy.persistentClass";
  private static final String ENTITY_NAME_FIELD_NAME = "org.hibernate.proxy.HibernateProxy.entityName";
  private static final String INTERFACES_FIELD_NAME = "org.hibernate.proxy.HibernateProxy.interfaces";
  private static final String ID_FIELD_NAME = "org.hibernate.proxy.HibernateProxy.id";
  private static final String SESSION_FIELD_NAME = "org.hibernate.proxy.HibernateProxy.session";
  private static final String GET_IDENTIFIER_METHOD_FIELD_NAME = "org.hibernate.proxy.HibernateProxy.getIdentifierMethod";
  private static final String SET_IDENTIFIER_METHOD_FIELD_NAME = "org.hibernate.proxy.HibernateProxy.setIdentifierMethod";
  private static final String COMPONENT_ID_TYPE_FIELD_NAME = "org.hibernate.proxy.HibernateProxy.componentIdType";
  
  public HibernateProxyApplicator(DNAEncoding encoding) {
    super(encoding);
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    Object lazyInitializer = getLazyInitializer(pojo);
    Object persistenceClass = getField("org.hibernate.proxy.pojo.BasicLazyInitializer", lazyInitializer, "persistentClass");
    Object entityName = getField("org.hibernate.proxy.AbstractLazyInitializer", lazyInitializer, "entityName");
    Object interfaces = getField("org.hibernate.proxy.pojo.cglib.CGLIBLazyInitializer", lazyInitializer, "interfaces");
    Object id = getField("org.hibernate.proxy.AbstractLazyInitializer", lazyInitializer, "id");
    //Object session = getField(AbstractLazyInitializer.class.getName(), lazyInitializer, "session");
    Object getIdentifierMethod = getField("org.hibernate.proxy.pojo.BasicLazyInitializer", lazyInitializer, "getIdentifierMethod");
    Object setIdentifierMethod = getField("org.hibernate.proxy.pojo.BasicLazyInitializer", lazyInitializer, "setIdentifierMethod");
    Object componentIdType = getField("org.hibernate.proxy.pojo.BasicLazyInitializer", lazyInitializer, "componentIdType");

    addTo.addAnonymousReference(persistenceClass);
    addTo.addAnonymousReference(entityName);
    addTo.addAnonymousReference(interfaces);
    addTo.addAnonymousReference(id);
    //addTo.addAnonymousReference(session);
    addTo.addAnonymousReference(getIdentifierMethod);
    addTo.addAnonymousReference(setIdentifierMethod);
    addTo.addAnonymousReference(componentIdType);
    return addTo;
  }

  private Object getLazyInitializer(Object pojo) {
    try {
      Class hibernateProxyClass = pojo.getClass().getClassLoader().loadClass("org.hibernate.proxy.HibernateProxy");
      Method m = hibernateProxyClass.getDeclaredMethod("getHibernateLazyInitializer", new Class[0]);
      m.setAccessible(true);
      return m.invoke(pojo, new Object[0]);
    } catch (ClassNotFoundException e) {
      throw new TCRuntimeException(e);
    } catch (SecurityException e) {
      throw new TCRuntimeException(e);
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

  private Object getField(String className, Object pojo, String fieldName) {
    try {
      Class loadedClazz = pojo.getClass().getClassLoader().loadClass(className);
      Field field = loadedClazz.getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(pojo);
    } catch (IllegalArgumentException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new TCRuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new TCRuntimeException(e);
    }
  }

  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object po) throws IOException,
      IllegalArgumentException, ClassNotFoundException {
    // 
  }

  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    Object lazyInitializer = getLazyInitializer(pojo);
    
    Object persistenceClass = getField("org.hibernate.proxy.pojo.BasicLazyInitializer", lazyInitializer, "persistentClass");
    Object entityName = getField("org.hibernate.proxy.AbstractLazyInitializer", lazyInitializer, "entityName");
    Object interfaces = getField("org.hibernate.proxy.pojo.cglib.CGLIBLazyInitializer", lazyInitializer, "interfaces");
    Object id = getField("org.hibernate.proxy.AbstractLazyInitializer", lazyInitializer, "id");
    //Object session = getField(AbstractLazyInitializer.class.getName(), lazyInitializer, "session");
    //session = getDehydratableObject(session, objectManager);
    Object getIdentifierMethod = getField("org.hibernate.proxy.pojo.BasicLazyInitializer", lazyInitializer, "getIdentifierMethod");
    getIdentifierMethod = getDehydratableObject(getIdentifierMethod, objectManager);
    Object setIdentifierMethod = getField("org.hibernate.proxy.pojo.BasicLazyInitializer", lazyInitializer, "setIdentifierMethod");
    setIdentifierMethod = getDehydratableObject(setIdentifierMethod, objectManager);
    Object componentIdType = getField("org.hibernate.proxy.pojo.BasicLazyInitializer", lazyInitializer, "componentIdType");
    componentIdType = getDehydratableObject(componentIdType, objectManager);


    writer.addPhysicalAction(PERSISTENT_CLASS_FIELD_NAME, persistenceClass);
    writer.addPhysicalAction(ENTITY_NAME_FIELD_NAME, entityName);
    writer.addPhysicalAction(INTERFACES_FIELD_NAME, interfaces);
    writer.addPhysicalAction(ID_FIELD_NAME, id);
    //writer.addPhysicalAction(SESSION_FIELD_NAME, session);
    writer.addPhysicalAction(GET_IDENTIFIER_METHOD_FIELD_NAME, getIdentifierMethod);
    writer.addPhysicalAction(SET_IDENTIFIER_METHOD_FIELD_NAME, setIdentifierMethod);
    writer.addPhysicalAction(COMPONENT_ID_TYPE_FIELD_NAME, componentIdType);
  }

  public Object getNewInstance(ClientObjectManager objectManager, DNA dna) throws IOException, ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    Assert.assertEquals(7, cursor.getActionCount());

    cursor.next(encoding);
    PhysicalAction a = cursor.getPhysicalAction();
    Class persistenceClass = (Class) a.getObject();
    
    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    String entityName = (String)a.getObject();
    
    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    Object[] interfacesObj = (Object[])a.getObject();
    Class[] interfaces = new Class[interfacesObj.length];
    System.arraycopy(interfacesObj, 0, interfaces, 0, interfacesObj.length);
    
    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    Serializable id = (Serializable)a.getObject();
    
    //cursor.next(encoding);
    //a = cursor.getPhysicalAction();
    //ObjectID sessionId = (ObjectID)a.getObject();
    //Object session = objectManager.lookupObject(sessionId);
    Object session = getSession(persistenceClass.getClassLoader());
    
    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    ObjectID getIdentifierId = (ObjectID)a.getObject();
    Method getIdentifierMethod = (Method)objectManager.lookupObject(getIdentifierId);
    
    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    ObjectID setIdentifierId = (ObjectID)a.getObject();
    Method setIdentifierMethod = (Method)objectManager.lookupObject(setIdentifierId);
    
    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    ObjectID componentIdTypeId = (ObjectID)a.getObject();
    Object componentIdType = objectManager.lookupObject(componentIdTypeId);
    
    Object hibernateProxy = createHibernateProxy(persistenceClass, entityName, interfaces, id, session, getIdentifierMethod, setIdentifierMethod, componentIdType);

    return hibernateProxy;
  }
  
  private Object getSession(ClassLoader loader) {
    try {
      Class hibernateUtilClass = loader.loadClass("org.terracotta.modules.hibernate_3_1_2.util.HibernateUtil");
      Method m = hibernateUtilClass.getDeclaredMethod("getSessionFactory", new Class[0]);
      Object sessionFactory = m.invoke(null, new Object[0]);
      
      Class sessionFactoryClass = sessionFactory.getClass();
      m = sessionFactoryClass.getDeclaredMethod("getCurrentSession", new Class[0]);
      Object session = m.invoke(sessionFactory, new Object[0]);
      
      return session;
    } catch (ClassNotFoundException e) {
      throw new TCRuntimeException(e);
    } catch (SecurityException e) {
      throw new TCRuntimeException(e);
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
  
  private Object createHibernateProxy(Class persistenceClass, String entityName, Class[] interfaces, Serializable id,
                                      Object session, Method getIdentifierMethod, Method setIdentifierMethod, Object componentIdType) {
    ClassLoader loader = persistenceClass.getClassLoader();
    
    try {
      Class lazyInitializerClass = loader.loadClass("org.hibernate.proxy.pojo.cglib.CGLIBLazyInitializer");
      Class abstractComponentTypeClass = loader.loadClass("org.hibernate.type.AbstractComponentType");
      Class sessionImplementorClass = loader.loadClass("org.hibernate.engine.SessionImplementor");
      
      Method getProxyMethod = lazyInitializerClass.getDeclaredMethod("getProxy",
                                                                     new Class[] {String.class, Class.class, Class[].class,
                                                                                  Method.class, Method.class, abstractComponentTypeClass,
                                                                                  Serializable.class, sessionImplementorClass});
      getProxyMethod.setAccessible(true);
      return getProxyMethod.invoke(null, new Object[]{entityName, persistenceClass, interfaces,
                                               getIdentifierMethod, setIdentifierMethod, componentIdType,
                                               id, session});
    } catch (ClassNotFoundException e) {
      throw new TCRuntimeException(e);
    } catch (SecurityException e) {
      throw new TCRuntimeException(e);
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

  public Map connectedCopy(Object source, Object dest, Map visited, ClientObjectManager objectManager,
                           OptimisticTransactionManager txManager) {
    throw new TCNotSupportedMethodException();
  }
}
