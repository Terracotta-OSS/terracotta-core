/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.event;

import com.tc.asm.Type;
import com.tc.cluster.ClusterEventListener;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ClientObjectManager;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObject;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.ReadOnlyException;
import com.tc.object.tx.WaitInvocation;
import com.tc.util.DebugUtil;
import com.tc.util.concurrent.StoppableThread;
import com.tcclient.object.Client;
import com.tcclient.object.DistributedMethodCall;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Manages distributed method calls
 */
public class DistributedMethodCallManagerImpl implements DistributedMethodCallManager, ClusterEventListener {
  private static final LiteralValues     literals    = new LiteralValues();
  private static final TCLogger          logger      = TCLogging.getLogger(DistributedMethodCallManagerImpl.class);

  private final static String            ROOT_NAME   = "@DistributeMethodCallRoot";
  private final static Object            ROOT_OBJECT = new Object();
  private final static long              WAIT_TIME   = 60 * 1000;
  // private final static long STOP_WAIT = 5 * 1000;

  private final ClientTransactionManager txManager;
  private final ClientObjectManager      objectManager;
  private Map                            clients;
  private Client                         myClient;
  private StoppableThread                invoker;

  private final RuntimeLogger            runtimeLogger;
  private final ClassProvider            classProvider;

  public DistributedMethodCallManagerImpl(ClientObjectManager objectManager, ClientTransactionManager txManager,
                                          RuntimeLogger runtimeLogger, ClassProvider classProvider) {
    super();
    this.objectManager = objectManager;
    this.txManager = txManager;
    this.runtimeLogger = runtimeLogger;
    this.classProvider = classProvider;
  }

  public void distributedInvoke(Object receiver, TCObject tcObject, String method, Object[] params) {
    if (Thread.currentThread() == invoker) return;

    begin(ROOT_NAME, LockLevel.WRITE);
    try {
      boolean[] refs = new boolean[params.length];
      for (int i = 0; i < params.length; i++) {
        Object param = params[i];
        if (literals.isLiteralInstance(param)) {
          refs[i] = false;
        } else {
          refs[i] = true;
          params[i] = new Long(objectManager.lookupOrCreate(param).getObjectID().toLong());
        }
      }

      Class recieverClass = receiver.getClass();
      String loaderDesc = classProvider.getLoaderDescriptionFor(recieverClass);

      DistributedMethodCall dmc = new DistributedMethodCall(tcObject.getObjectID().toLong(), recieverClass.getName(),
                                                            loaderDesc, method, params, refs);

      if (runtimeLogger.distributedMethodDebug()) {
        runtimeLogger.distributedMethodCall(dmc.getReceiverClassName(), dmc.getMethodName(), dmc.getParameterDesc());
      }

      for (Iterator i = clients.values().iterator(); i.hasNext();) {
        Client c = (Client) i.next();
        if (c != myClient) {
          c.add(dmc);
        }
      }
      txManager.notify(ROOT_NAME, true, ROOT_OBJECT);
    } finally {
      commit(ROOT_NAME);
    }
  }

  public synchronized void stop(boolean immediate) {
    if (invoker != null) {
      try {
        invoker.requestStop();

        // XXX: (TE) This stuff needs to be cleaned up
        if (false) {
          if (!immediate) {
            // notify the shared object (ie. help us shutdown faster w/o waiting for the distributed wait() to unlock)
            begin(ROOT_NAME, LockLevel.WRITE);
            try {
              // what I really want to is a "local-only" distributed notify(). I only want to wake up
              // the one wait()'er that is local
              txManager.notify(ROOT_NAME, true, ROOT_OBJECT);
            } finally {
              commit(ROOT_NAME);
            }
          }
        }

        // invoker.stopAndWait(STOP_WAIT);
      } finally {
        if (invoker.isAlive()) {
          logger.warn("Listener thread still alive");
        }
        invoker = null;
      }
    }
  }

  private void begin(String lockName, int type) {
    txManager.begin(lockName, type);
  }

  private void monitorEnter(Object obj, int type) {
    txManager.begin(ByteCodeUtil.generateAutolockName(objectManager.lookupOrCreate(obj).getObjectID()), type);
  }

  private void commit(String lockName) {
    txManager.commit(lockName);
  }

  public void nodeConnected(String nodeId) {
    // NOTE: no-op
  }

  public void nodeDisconnected(String nodeId) {
    begin(ROOT_NAME, LockLevel.WRITE);
    try {
      clients.remove(nodeId);
    } finally {
      commit(ROOT_NAME);
    }
  }

  public synchronized void thisNodeConnected(String thisNodeId, String[] nodesCurrentlyInCluster) {
    if (invoker == null) {
      begin(ROOT_NAME, LockLevel.WRITE);
      try {
        clients = (Map) objectManager.lookupOrCreateRoot(ROOT_NAME, new HashMap());
        myClient = new Client();
        clients.put(thisNodeId, myClient);

        // XXX: This is a complete hack. Within the context of the DSO test framework, the calling thread
        // will not have it's DSO context established yet and will thus call on NullManager which will
        // discard the logicalInvoke
        if (ClassProcessorHelper.getManager(Thread.currentThread().getContextClassLoader()) == null) {
          txManager.logicalInvoke(((Manageable) clients).__tc_managed(), SerializationUtil.PUT, "put(Object,Object)",
                                  new Object[] { thisNodeId, myClient });
        }

      } finally {
        commit(ROOT_NAME);
      }

      invoker = new InvokerThread();
      invoker.start();
    }
  }

  public void thisNodeDisconnected(String thisNodeId) {
    // NOTE: no-op
  }

  private class InvokerThread extends StoppableThread {
    private final ClassLoader origContextLoader;

    InvokerThread() {
      super("DistributedMethodInvoke");
      setDaemon(true);
      this.origContextLoader = getContextClassLoader();
    }

    public void run() {
      while (true) {
        DistributedMethodCall dmc = null;

        begin(ROOT_NAME, LockLevel.WRITE);
        try {
          while (myClient.isEmpty()) {
            if (isStopRequested()) { return; }
            txManager.wait(ROOT_NAME, new WaitInvocation(WAIT_TIME), clients);
          }
          dmc = myClient.next();
        } catch (InterruptedException ie) {
          ie.printStackTrace();
          throw new AssertionError(ie);
        } finally {
          commit(ROOT_NAME);
        }

        if (runtimeLogger.distributedMethodDebug()) {
          runtimeLogger.distributedMethodCall(dmc.getReceiverClassName(), dmc.getMethodName(), dmc.getParameterDesc());
        }

        // This bit of wierdness is to get the target object and the method arguments faulted in to this VM. A side of
        // effect of loading these objects is that it will test whether the appropriate classes for the objects can
        // actually be loaded locally (The node that created the distributed method call might have totally different
        // classes compared to what is available locally)
        Object receiver;
        Object[] parameters;
        try {
          classProvider.getClassFor(dmc.getReceiverClassName(), dmc.getReceiverClassLoaderDesc());
          receiver = resolveReceiver(dmc);
          parameters = resolveParameters(dmc);
        } catch (Exception e) {
          if (logger.isDebugEnabled()) {
            logger.debug("Ignoring distributed method call", e);
          }
          continue;
        }

        monitorEnter(dmc, LockLevel.READ);
        try {

          if (DebugUtil.DEBUG) {
            System.err.println("In DistributedMethodCallManager -- client id: " + ManagerUtil.getClientID()
                               + " running method " + dmc.getMethodName());
          }
          invokeMethod(dmc, receiver, parameters);
        } catch (Throwable e) {
          runtimeLogger.distributedMethodCallError(dmc.getReceiverClassName(), dmc.getMethodName(), dmc
              .getParameterDesc(), e);
        } finally {
          commit(ByteCodeUtil.generateAutolockName(objectManager.lookupOrCreate(dmc).getObjectID()));
        }
      }
    }

    private Object[] resolveParameters(DistributedMethodCall dmc) {
      Object[] params = dmc.getParameters();

      for (int i = 0; i < params.length; i++) {
        if (dmc.isRef(i)) {
          Long id = (Long) params[i];
          params[i] = objectManager.lookupObject(new ObjectID(id.longValue()));
        }
      }

      return params;
    }

    private Object resolveReceiver(DistributedMethodCall dmc) {
      return objectManager.lookupObject(new ObjectID(dmc.getReceiverID()));
    }

    private void invokeMethod(DistributedMethodCall dmc, Object receiver, Object[] parameters) throws Throwable {
      Method m = getMethod(dmc, receiver);
      m.setAccessible(true);
      try {
        Thread.currentThread().setContextClassLoader(receiver.getClass().getClassLoader());
        m.invoke(receiver, parameters);
      } catch (InvocationTargetException e) {
        if (!(e.getCause() instanceof ReadOnlyException)) { throw e; }
        throw new ReadOnlyException(
                                    "A distributed method call with read-only access attempted to modify a shared object.  The method call was not executed.  "
                                        + "\nDistributed methods are given read-only access automatically unless a read/write lock is explicitly indicated.  "
                                        + "\nPlease alter the locks section of your Terracotta configuration so that this distributed method has read/write access.",
                                    Thread.currentThread().getName(), txManager.getChannelIDProvider().getChannelID()
                                        .toLong(), "Failed Distributed Method Call: " + m.getName());
      } finally {
        Thread.currentThread().setContextClassLoader(origContextLoader);
      }
    }

    private Method getMethod(DistributedMethodCall dmc, Object receiver) {
      String methodName = dmc.getMethodName();
      String paramDesc = dmc.getParameterDesc();

      Class c = receiver.getClass();

      while (c != null) {
        Method[] methods = c.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
          Method m = methods[i];
          if (!m.getName().equals(methodName)) {
            continue;
          }
          Class[] argTypes = m.getParameterTypes();
          StringBuffer signature = new StringBuffer("(");
          for (int j = 0; j < argTypes.length; j++) {
            signature.append(Type.getDescriptor(argTypes[j]));
          }
          signature.append(")");
          signature.append(Type.getDescriptor(m.getReturnType()));
          if (signature.toString().equals(paramDesc)) { return m; }
        }

        c = c.getSuperclass();
      }
      throw new RuntimeException("Method " + methodName + paramDesc + " does not exist on this object: " + receiver);
    }

  }

}
