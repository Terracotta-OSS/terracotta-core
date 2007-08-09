/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx.optimistic;

import com.tc.object.ClientObjectManager;
import com.tc.object.LiteralValues;
import com.tc.object.TCObject;
import com.tc.object.change.TCChangeBuffer;
import com.tc.object.change.TCChangeBufferEventVisitor;
import com.tc.object.change.event.ArrayElementChangeEvent;
import com.tc.object.change.event.LogicalChangeEvent;
import com.tc.object.change.event.PhysicalChangeEvent;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;

import java.util.Iterator;
import java.util.Map;

public class OptimisticTransactionManagerImpl implements OptimisticTransactionManager {
  private ThreadLocal              transaction   = new ThreadLocal();
  private ClientObjectManager      objectManager;
  private ClientTransactionManager clientTxManager;
  private LiteralValues            literalValues = new LiteralValues();

  public OptimisticTransactionManagerImpl(ClientObjectManager objectManager, ClientTransactionManager clientTxManager) {
    this.objectManager = objectManager;
    this.clientTxManager = clientTxManager;
    Assert.eval(objectManager != null);
  }

  public void begin() {
    if (getTransaction() != null) { throw new AssertionError("Don't support nested optimistic transactions"); }

    transaction.set(new OptimisticTransaction());
  }

  public void objectFieldChanged(TCObject clone, String classname, String fieldname, Object newValue, int index) {
    getTransaction().objectFieldChanged(clone, classname, fieldname, newValue, index);
  }

  public void logicalInvoke(TCObject clone, int method, String methodName, Object[] parameters) {
    getTransaction().logicalInvoke(clone, method, parameters);
  }

  public void rollback() {
    if (getTransaction() != null) { throw new AssertionError("Can't rollback non-exsistent tx"); }
    transaction.set(null);
  }

  public Object convertToParameter(Object obj) {
    Object nv = null;
    OptimisticTransaction ot = getTransaction();
    ClientTransaction ctx = clientTxManager.getTransaction();

    if (ot.hasClone(obj)) {
      TCObject ctcobj = ot.getTCObjectFor(obj);
      nv = ctcobj.getObjectID();
    } else if (literalValues.isLiteralInstance(obj)) {
      nv = obj;
    } else {
      TCObject ctcobj = objectManager.lookupOrCreate(obj);
      ctx.createObject(ctcobj);
      nv = ctcobj.getObjectID();
    }
    return nv;
  }

  public void commit() throws ClassNotFoundException {
    OptimisticTransaction ot = getTransaction();
    final ClientTransaction ctx = clientTxManager.getTransaction();
    Map buffers = ot.getChangeBuffers();
    for (Iterator i = buffers.values().iterator(); i.hasNext();) {
      TCChangeBuffer buf = (TCChangeBuffer) i.next();
      Assert.eval(buf.getTCObject() != null);
      final TCObject tcobj = objectManager.lookup(buf.getTCObject().getObjectID());
      try {
        tcobj.hydrate(new DNAToChangeBufferBridge(this, buf), true);
      } catch (ClassNotFoundException e) {
        throw e;
      }

      // Add the changes to the dso transaction
      buf.accept(new TCChangeBufferEventVisitor() {
          public void visitPhysicalChangeEvent(PhysicalChangeEvent pe) {
            Object nv = convertToParameter(pe.getNewValue());
            Assert.eval(nv != null);
            ctx.fieldChanged(tcobj, tcobj.getTCClass().getName(), pe.getFieldName(), nv, -1);
          }

          public void visitLogicalEvent(LogicalChangeEvent lce) {
            Object[] params = lce.getParameters();
            Object[] newParams = new Object[params.length];
            for (int j = 0; j < newParams.length; j++) {
              newParams[j] = convertToParameter(params[j]);
            }
            ctx.logicalInvoke(tcobj, lce.getMethodID(), newParams, "");
          }

          public void visitArrayElementChangeEvent(ArrayElementChangeEvent ae) {
            Object nv = ae.getValue();
            if (ae.isSubarray()) {
              if (!ClassUtils.isPrimitiveArray(nv)) {
                Object[] values = (Object[]) nv;
                for (int j = 0; j < values.length; j++) {
                  values[j] = convertToParameter(values[j]);
                }
              }
              ctx.arrayChanged(tcobj, ae.getIndex(), nv, ae.getLength());
            } else {
              ctx.fieldChanged(tcobj, tcobj.getTCClass().getName(), null, convertToParameter(nv), ae.getIndex());
            }
          }
        });
    }
    transaction.set(null);
  }

  private OptimisticTransaction getTransaction() {
    return (OptimisticTransaction) transaction.get();
  }

  public void addClonesToTransaction(Map cloned) {
    getTransaction().addAll(cloned);
  }

}
