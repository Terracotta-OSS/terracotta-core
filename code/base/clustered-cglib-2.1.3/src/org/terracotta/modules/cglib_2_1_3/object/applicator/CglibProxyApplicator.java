/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.cglib_2_1_3.object.applicator;

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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;

public class CglibProxyApplicator extends BaseApplicator {
  private static final String SUPERCLASS_FIELD_NAME  = "net.sf.cglib.proxy.Factory.superclass";
  private static final String CALLBACK_FIELD_NAME    = "net.sf.cglib.proxy.Factory.callBack";

  public CglibProxyApplicator(DNAEncoding encoding) {
    super(encoding);
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    addTo.addAnonymousReference(getCallBack(pojo));
    return addTo;
  }
  
  private Object getCallBack(Object pojo) {
    return get(pojo, "getCallback", new Class[]{Integer.TYPE}, new Object[]{new Integer(0)});
  }
  
  private Object get(Object pojo, String methodName, Class[] parameterTypes, Object[] parameterValues) {
    try {
      Method m = pojo.getClass().getDeclaredMethod(methodName, parameterTypes);
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
    // TODO
  }

  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    Object callBack = getCallBack(pojo);
    Object dehydratableCallBack = getDehydratableObject(callBack, objectManager);

    Class superClass = pojo.getClass().getSuperclass();

    writer.addPhysicalAction(SUPERCLASS_FIELD_NAME, superClass);
    writer.addPhysicalAction(CALLBACK_FIELD_NAME, dehydratableCallBack);
  }

  public Object getNewInstance(ClientObjectManager objectManager, DNA dna) throws IOException, ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    Assert.assertEquals(2, cursor.getActionCount());

    cursor.next(encoding);
    PhysicalAction a = cursor.getPhysicalAction();
    Class superClass = (Class) a.getObject();

    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    Object callBack = a.getObject();

    callBack = objectManager.lookupObject((ObjectID) callBack);

    //return Enhancer.create(superClass, (Callback) callBack);
    return create(superClass, callBack);
  }
  
  private Object create(Class superClass, Object callBack) {
    try {
      Class enhancerClass = superClass.getClassLoader().loadClass("net.sf.cglib.proxy.Enhancer");
      Class callbackClass = callBack.getClass().getClassLoader().loadClass("net.sf.cglib.proxy.Callback");
      Method m = enhancerClass.getDeclaredMethod("create", new Class[]{ Class.class, callbackClass } );
      Object o = m.invoke(null, new Object[]{ superClass, callBack });
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
    Map cloned = new IdentityHashMap();

    // TODO

    return cloned;
  }
}
