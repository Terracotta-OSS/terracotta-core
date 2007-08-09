/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.IDNAEncoding;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;
import com.tc.object.tx.optimistic.TCObjectClone;
import com.tc.util.Assert;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;
import java.util.Map;

public class ProxyApplicator extends BaseApplicator {
  private static final String CLASSLOADER_FIELD_NAME        = "java.lang.reflect.Proxy.loader";
  private static final String INTERFACES_FIELD_NAME         = "java.lang.reflect.Proxy.interfaces";
  private static final String INVOCATION_HANDLER_FIELD_NAME = "java.lang.reflect.Proxy.h";

  public ProxyApplicator(IDNAEncoding encoding) {
    super(encoding);
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    addTo.addAnonymousReference(Proxy.getInvocationHandler(pojo));
    return addTo;
  }

  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object po) throws IOException,
      IllegalArgumentException, ClassNotFoundException {
    // Most of the time, hydrate() of ProxyApplicator will not be needed as we create
    // instance of a Proxy instance using the getNewInstance() method. This is being
    // called only when someone is modifying the invocation handler field of a proxy using
    // reflection.
    DNACursor cursor = dna.getCursor();
    String fieldName;
    Object fieldValue;

    while (cursor.next(encoding)) {
      PhysicalAction a = cursor.getPhysicalAction();
      Assert.eval(a.isTruePhysical());
      fieldName = a.getFieldName();
      fieldValue = a.getObject();
      
      if (fieldName.equals(INVOCATION_HANDLER_FIELD_NAME)) {
        fieldValue = objectManager.lookupObject((ObjectID) fieldValue);
        ((TransparentAccess) po).__tc_setfield(fieldName, fieldValue);
      }
    }
  }

  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    InvocationHandler handler = Proxy.getInvocationHandler(pojo);
    Object dehydratableHandler = getDehydratableObject(handler, objectManager);

    //writer.addPhysicalAction(CLASSLOADER_FIELD_NAME, pojo.getClass().getClassLoader());
    writer.addClassLoaderAction(CLASSLOADER_FIELD_NAME, pojo.getClass().getClassLoader());
    writer.addPhysicalAction(INTERFACES_FIELD_NAME, pojo.getClass().getInterfaces());
    writer.addPhysicalAction(INVOCATION_HANDLER_FIELD_NAME, dehydratableHandler);
  }

  public Object getNewInstance(ClientObjectManager objectManager, DNA dna) throws IOException, ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    Assert.assertEquals(3, cursor.getActionCount());

    cursor.next(encoding);
    PhysicalAction a = cursor.getPhysicalAction();
    ClassLoader loader = (ClassLoader) a.getObject();

    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    Object[] values = (Object[]) a.getObject();
    Class[] interfaces = new Class[values.length];
    System.arraycopy(values, 0, interfaces, 0, values.length);

    cursor.next(encoding);
    a = cursor.getPhysicalAction();
    Object handler = a.getObject();

    handler = objectManager.lookupObject((ObjectID) handler);

    return Proxy.newProxyInstance(loader, interfaces, (InvocationHandler) handler);
  }

  public Map connectedCopy(Object source, Object dest, Map visited, ClientObjectManager objectManager,
                           OptimisticTransactionManager txManager) {
    Map cloned = new IdentityHashMap();

    Proxy srcProxy = (Proxy) source;
    Proxy destProxy = (Proxy) dest;

    InvocationHandler handler = Proxy.getInvocationHandler(srcProxy);
    InvocationHandler clonedHandler = (InvocationHandler) createCopyIfNecessary(objectManager, visited, cloned, handler);
    
    ((TransparentAccess)destProxy).__tc_setfield(INVOCATION_HANDLER_FIELD_NAME, clonedHandler);

    TCObject srcTCObject = ((Manageable)srcProxy).__tc_managed();
    ((Manageable)destProxy).__tc_managed(new TCObjectClone(srcTCObject, txManager));

    return cloned;
  }
}
