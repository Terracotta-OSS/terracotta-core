/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.ParseException;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.field.TCField;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.Util;

import gnu.trove.TLinkable;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;

/**
 * Implementation of TCObject interface.
 * <p>
 */
public abstract class TCObjectImpl implements TCObject {
  private static final TCLogger logger                      = TCLogging.getLogger(TCObjectImpl.class);

  private static final int      ACCESSED_OFFSET             = 1;
  private static final int      IS_NEW_OFFSET               = 2;
  private static final int      AUTOLOCKS_DISABLED_OFFSET   = 4;
  private static final int      EVICTION_IN_PROGRESS_OFFSET = 8;

  private long                  version                     = 0;

  private final ObjectID        objectID;
  protected final TCClass       tcClazz;
  private WeakObjectReference   peerObject;
  private TLinkable             next;
  private TLinkable             previous;
  private byte                  flags                       = 0;
  private static final TCLogger consoleLogger               = CustomerLogging.getConsoleLogger();

  protected TCObjectImpl(ReferenceQueue queue, ObjectID id, Object peer, TCClass clazz) {
    this.objectID = id;
    this.tcClazz = clazz;
    setPeerObject(new WeakObjectReference(id, peer, queue));
  }

  public boolean isShared() {
    return true;
  }

  public boolean isNull() {
    return peerObject == null || getPeerObject() == null;
  }

  public ObjectID getObjectID() {
    return objectID;
  }

  protected ClientObjectManager getObjectManager() {
    return tcClazz.getObjectManager();
  }

  public Object getPeerObject() {
    return peerObject.get();
  }

  protected void setPeerObject(WeakObjectReference pojo) {
    this.peerObject = pojo;
    Object realPojo;
    if ((realPojo = peerObject.get()) instanceof Manageable) {
      Manageable m = (Manageable) realPojo;
      m.__tc_managed(this);
    }
  }

  public TCClass getTCClass() {
    return tcClazz;
  }

  /**
   * Reconstitutes the object using the data in the DNA strand. XXX: We may need to signal (via a different signature or
   * args) that the hydration is intended to initialize the object from scratch or if it's a delta. We must avoid
   * creating a new instance of the peer object if the strand is just a delta.
   * 
   * @throws ClassNotFoundException
   */
  public void hydrate(DNA from, boolean force) throws ClassNotFoundException {
    synchronized (getResolveLock()) {
      boolean isNewLoad = isNull();
      createPeerObjectIfNecessary(from);

      Object po = getPeerObject();
      if (po == null) return;
      try {
        tcClazz.hydrate(this, from, po, force);
        if (isNewLoad) performOnLoadActionIfNecessary(po);
      } catch (ClassNotFoundException e) {
        throw e;
      } catch (IOException e) {
        throw new DNAException(e);
      } 
    }
  }

  private void performOnLoadActionIfNecessary(Object pojo) {
    TCClass tcc = getTCClass();
    if (tcc.hasOnLoadExecuteScript() || tcc.hasOnLoadMethod()) {
      String eval = tcc.hasOnLoadExecuteScript() ? tcc.getOnLoadExecuteScript() : "self." + tcc.getOnLoadMethod()
                                                                                  + "()";
      resolveAllReferences();

      ClassLoader prevLoader = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(pojo.getClass().getClassLoader());
      try {
        Interpreter i = new Interpreter();
        i.setClassLoader(tcc.getPeerClass().getClassLoader());
        i.set("self", pojo);
        i.eval("setAccessibility(true)");
        i.eval(eval);
      } catch (ParseException e) {
        // Error Parsing script. Use e.getMessage() instead of e.getErrorText() when there is a ParseException because
        // expectedTokenSequences in ParseException could be null and thus, may throw a NullPointerException when
        // calling
        // e.getErrorText().
        consoleLogger.error("Unable to parse OnLoad script: " + pojo.getClass() + " error: " + e.getMessage()
                            + " stack: " + e.getScriptStackTrace());
        logger.error("Unable to parse OnLoad script: " + pojo.getClass() + " error: " + e.getMessage() + " line: "
                     + " stack: " + e.getScriptStackTrace());
      } catch (EvalError e) {
        // General Error evaluating script
        consoleLogger.error("OnLoad execute script failed for: " + pojo.getClass() + " error: " + e.getErrorText()
                            + " line: " + e.getErrorLineNumber() + "; " + e.getMessage() + "; stack: "
                            + e.getScriptStackTrace());
        logger.error("OnLoad execute script failed for: " + pojo.getClass() + " error: " + e.getErrorText() + " line: "
                     + e.getErrorLineNumber() + "; " + e.getMessage() + "; stack: " + e.getScriptStackTrace());
      } finally {
        Thread.currentThread().setContextClassLoader(prevLoader);
      }
    }
  }

  protected synchronized void setFlag(int offset, boolean value) {
    flags = Conversion.setFlag(flags, offset, value);
  }

  private synchronized boolean getFlag(int offset) {
    return Conversion.getFlag(flags, offset);
  }

  private void createPeerObjectIfNecessary(DNA from) {
    if (isNull()) {
      // TODO: set created and modified version id
      setPeerObject(getObjectManager().createNewPeer(tcClazz, from));
    }
  }

  public void setReference(String fieldName, ObjectID id) {
    throw new AssertionError("shouldn't be called");
  }

  public void setValue(String fieldName, Object obj) {
    try {
      TransparentAccess ta = (TransparentAccess) getPeerObject();
      if (ta == null) {
        // Object was GC'd so return which should lead to a re-retrieve
        return;
      }
      clearReference(fieldName);
      TCField field = getTCClass().getField(fieldName);
      if (field == null) {
        logger.warn("Data for field:" + fieldName + " was recieved but that field does not exist in class:");
        return;
      }
      if (obj instanceof ObjectID) {
        setReference(fieldName, (ObjectID) obj);
        if (!field.isFinal()) {
          ta.__tc_setfield(field.getName(), null);
        }
      } else {
        // clean this up
        ta.__tc_setfield(field.getName(), obj);
      }
    } catch (Exception e) {
      // TODO: More elegant exception handling.
      throw new com.tc.object.dna.api.DNAException(e);
    }
  }

  public int clearReferences(int toClear) {
    synchronized (getResolveLock()) {
      try {
        Object po = getPeerObject();
        Assert.assertFalse(isNew()); // Shouldnt clear new Objects
        if (po == null) return 0;
        return clearReferences(po, toClear);
      } finally {
        setEvictionInProgress(false);
      }
    }
  }

  protected abstract int clearReferences(Object pojo, int toClear);

  public final Object getResolveLock() {
    return objectID; // Save a field by using this one as the lock
  }

  public void resolveArrayReference(int index) {
    throw new AssertionError("shouldn't be called");
  }

  public void clearArrayReference(int index) {
    clearReference(Integer.toString(index));
  }

  public void clearReference(String fieldName) {
    // do nothing
  }

  public void resolveReference(String fieldName) {
    // do nothing
  }

  public void resolveAllReferences() {
    // override me
  }

  public void literalValueChanged(Object newValue, Object oldValue) {
    throw new UnsupportedOperationException();
  }

  public void setLiteralValue(Object newValue) {
    throw new UnsupportedOperationException();
  }

  /**
   * Writes the data in the object to the DNA writer supplied.
   */
  public void dehydrate(DNAWriter writer) throws DNAException {
    tcClazz.dehydrate(this, writer, getPeerObject());
  }

  public synchronized void setVersion(long version) {
    this.version = version;
  }

  public synchronized long getVersion() {
    return this.version;
  }

  public String toString() {
    return getClass().getName() + "@" + System.identityHashCode(this) + "[objectID=" + objectID + ", TCClass="
           + tcClazz + "]";
  }

  public void objectFieldChanged(String classname, String fieldname, Object newValue, int index) {
    try {
      this.markAccessed();
      if (index == NULL_INDEX) {
        // Assert.eval(fieldname.indexOf('.') >= 0);
        clearReference(fieldname);
      } else {
        clearArrayReference(index);
      }
      getObjectManager().getTransactionManager().fieldChanged(this, classname, fieldname, newValue, index);
    } catch (Throwable t) {
      Util.printLogAndRethrowError(t, logger);
    }
  }

  public void objectFieldChangedByOffset(String classname, long fieldOffset, Object newValue, int index) {
    String fieldname = tcClazz.getFieldNameByOffset(fieldOffset);
    objectFieldChanged(classname, fieldname, newValue, index);
  }

  public String getFieldNameByOffset(long fieldOffset) {
    return tcClazz.getFieldNameByOffset(fieldOffset);
  }

  public void booleanFieldChanged(String classname, String fieldname, boolean newValue, int index) {
    objectFieldChanged(classname, fieldname, new Boolean(newValue), index);
  }

  public void byteFieldChanged(String classname, String fieldname, byte newValue, int index) {
    objectFieldChanged(classname, fieldname, new Byte(newValue), index);
  }

  public void charFieldChanged(String classname, String fieldname, char newValue, int index) {
    objectFieldChanged(classname, fieldname, new Character(newValue), index);
  }

  public void doubleFieldChanged(String classname, String fieldname, double newValue, int index) {
    objectFieldChanged(classname, fieldname, new Double(newValue), index);
  }

  public void floatFieldChanged(String classname, String fieldname, float newValue, int index) {
    objectFieldChanged(classname, fieldname, new Float(newValue), index);
  }

  public void intFieldChanged(String classname, String fieldname, int newValue, int index) {
    objectFieldChanged(classname, fieldname, new Integer(newValue), index);
  }

  public void longFieldChanged(String classname, String fieldname, long newValue, int index) {
    objectFieldChanged(classname, fieldname, new Long(newValue), index);
  }

  public void shortFieldChanged(String classname, String fieldname, short newValue, int index) {
    objectFieldChanged(classname, fieldname, new Short(newValue), index);
  }

  public void objectArrayChanged(int startPos, Object[] array, int length) {
    this.markAccessed();
    for (int i = 0; i < length; i++) {
      clearArrayReference(startPos + i);
    }
    getObjectManager().getTransactionManager().arrayChanged(this, startPos, array, length);
  }

  public void primitiveArrayChanged(int startPos, Object array, int length) {
    this.markAccessed();
    getObjectManager().getTransactionManager().arrayChanged(this, startPos, array, length);
  }

  public void setNext(TLinkable link) {
    this.next = link;
  }

  public void setPrevious(TLinkable link) {
    this.previous = link;
  }

  public TLinkable getNext() {
    return this.next;
  }

  public TLinkable getPrevious() {
    return this.previous;
  }

  public void markAccessed() {
    setFlag(ACCESSED_OFFSET, true);
  }

  public void clearAccessed() {
    setFlag(ACCESSED_OFFSET, false);
  }

  public boolean recentlyAccessed() {
    return getFlag(ACCESSED_OFFSET);
  }

  public int accessCount(int factor) {
    // TODO:: Implement when needed
    throw new UnsupportedOperationException();
  }

  public synchronized boolean getAndResetNew() {
    if (getFlag(IS_NEW_OFFSET)) {
      setFlag(IS_NEW_OFFSET, false);
      return true;
    }
    return false;
  }

  public synchronized void setIsNew() {
    if (getFlag(IS_NEW_OFFSET)) { throw new IllegalStateException("new flag already set"); }
    setFlag(IS_NEW_OFFSET, true);
  }

  public boolean isNew() {
    return getFlag(IS_NEW_OFFSET);
  }

  // These autlocking disable methods are checked in ManagerImpl. The one known use case
  // is the Hashtable used to hold sessions. We need local synchronization,
  // but we don't ever want autolocks for that particular instance
  public void disableAutoLocking() {
    setFlag(AUTOLOCKS_DISABLED_OFFSET, true);
  }

  public boolean autoLockingDisabled() {
    return getFlag(AUTOLOCKS_DISABLED_OFFSET);
  }

  private void setEvictionInProgress(boolean value) {
    setFlag(EVICTION_IN_PROGRESS_OFFSET, value);
  }

  private boolean isEvictionInProgress() {
    return getFlag(EVICTION_IN_PROGRESS_OFFSET);
  }

  public synchronized boolean canEvict() {
    boolean canEvict = isEvictable() && !(isNew() || isEvictionInProgress());
    if (canEvict) {
      setEvictionInProgress(true);
    }
    return canEvict;
  }

  protected abstract boolean isEvictable();

}
