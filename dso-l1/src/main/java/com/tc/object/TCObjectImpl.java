/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.ParseException;
import bsh.TargetError;

import com.tc.injection.DsoClusterInjectionInstrumentation;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.field.TCField;
import com.tc.object.util.ToggleableStrongReference;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.Util;

import gnu.trove.TLinkable;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of TCObject interface.
 */
public abstract class TCObjectImpl implements TCObject {
  private static final TCLogger logger                    = TCLogging.getLogger(TCObjectImpl.class);

  private static final int      ACCESSED_OFFSET           = 1 << 0;
  private static final int      IS_NEW_OFFSET             = 1 << 1;
  private static final int      AUTOLOCKS_DISABLED_OFFSET = 1 << 2;

  // This initial negative version number is important since GID is assigned in the server from 0.
  private long                  version                   = -1;

  protected final ObjectID      objectID;
  protected final TCClass       tcClazz;
  private WeakReference         peerObject;
  private byte                  flags                     = 0;
  private static final TCLogger consoleLogger             = CustomerLogging.getConsoleLogger();

  protected TCObjectImpl(final ObjectID id, final Object peer, final TCClass clazz, final boolean isNew) {
    this.objectID = id;
    this.tcClazz = clazz;
    if (peer != null) {
      setPeerObject(getObjectManager().newWeakObjectReference(id, peer));
    }

    setFlag(IS_NEW_OFFSET, isNew);
  }

  public boolean isShared() {
    return true;
  }

  public boolean isNull() {
    return this.peerObject == null || getPeerObject() == null;
  }

  public ObjectID getObjectID() {
    return this.objectID;
  }

  protected ClientObjectManager getObjectManager() {
    return this.tcClazz.getObjectManager();
  }

  public Object getPeerObject() {
    return this.peerObject == null ? null : this.peerObject.get();
  }

  protected void setPeerObject(final WeakReference pojo) {
    this.peerObject = pojo;
  }

  public TCClass getTCClass() {
    return this.tcClazz;
  }

  public void dehydrate(final DNAWriter writer) {
    this.tcClazz.dehydrate(this, writer, getPeerObject());
  }

  /**
   * Reconstitutes the object using the data in the DNA strand. XXX: We may need to signal (via a different signature or
   * args) that the hydration is intended to initialize the object from scratch or if it's a delta. We must avoid
   * creating a new instance of the peer object if the strand is just a delta.<br>
   * <p>
   * TODO:: Split into two interface, peer is null if not new.
   * 
   * @throws ClassNotFoundException
   */
  public void hydrate(final DNA from, final boolean force, final WeakReference peer) throws ClassNotFoundException {
    synchronized (getResolveLock()) {
      if (peer != null) {
        setPeerObject(peer);
      }
      final Object po = getPeerObject();
      if (po == null) { return; }
      try {
        this.tcClazz.hydrate(this, from, po, force);
        if (peer != null) {
          // This is newly created object, load action
          performOnLoadActionIfNecessary(po);
        }
      } catch (final ClassNotFoundException e) {
        logger.warn("Re-throwing Exception: ", e);
        throw e;
      } catch (final IOException e) {
        logger.warn("Re-throwing Exception: ", e);
        throw new DNAException(e);
      }
    }
  }

  private void performOnLoadActionIfNecessary(final Object pojo) {
    final TCClass tcc = getTCClass();
    if (tcc.hasOnLoadInjection() || tcc.hasOnLoadExecuteScript() || tcc.hasOnLoadMethod()) {
      String eval = "";

      if (tcc.hasOnLoadInjection()) {
        eval += "self." + DsoClusterInjectionInstrumentation.TC_INJECTION_METHOD_NAME + "();\n";
      }

      if (tcc.hasOnLoadExecuteScript()) {
        eval += tcc.getOnLoadExecuteScript();
      } else if (tcc.hasOnLoadMethod()) {
        eval += "self." + tcc.getOnLoadMethod() + "()";
      }

      resolveAllReferences();

      final ClassLoader prevLoader = Thread.currentThread().getContextClassLoader();
      final boolean adjustTCL = TCThreadGroup.currentThreadInTCThreadGroup();

      if (adjustTCL) {
        ClassLoader newTCL = pojo.getClass().getClassLoader();
        if (newTCL == null) {
          newTCL = ClassLoader.getSystemClassLoader();
        }
        Thread.currentThread().setContextClassLoader(newTCL);
      }

      try {
        final Interpreter i = new Interpreter();
        i.setClassLoader(tcc.getPeerClass().getClassLoader());
        i.set("self", pojo);
        try {
          i.eval("setAccessibility(true)");
          i.eval(eval);
        } finally {
          i.getNameSpace().clear();
        }
      } catch (final ParseException e) {
        // Error Parsing script. Use e.getMessage() instead of e.getErrorText() when there is a ParseException because
        // expectedTokenSequences in ParseException could be null and thus, may throw a NullPointerException when
        // calling
        // e.getErrorText().
        consoleLogger.error("Unable to parse OnLoad script: " + pojo.getClass() + " error: " + e.getMessage()
                            + " stack: " + e.getScriptStackTrace());
      } catch (final EvalError e) {
        // General Error evaluating script
        Throwable cause = null;
        if (e instanceof TargetError) {
          cause = ((TargetError) e).getTarget();
        }

        final String errorMsg = "OnLoad execute script failed for: " + pojo.getClass() + " error: " + e.getErrorText()
                                + " line: " + e.getErrorLineNumber() + "; " + e.getMessage();

        if (cause != null) {
          consoleLogger.error(errorMsg, cause);
        } else {
          consoleLogger.error(errorMsg);
        }
      } finally {
        if (adjustTCL) {
          Thread.currentThread().setContextClassLoader(prevLoader);
        }
      }
    }
  }

  private synchronized void setFlag(final int offset, final boolean value) {
    this.flags = Conversion.setFlag(this.flags, offset, value);
  }

  private synchronized boolean getFlag(final int offset) {
    return Conversion.getFlag(this.flags, offset);
  }

  private synchronized boolean compareAndSetFlag(final int offset, final boolean old, final boolean newValue) {
    if (Conversion.getFlag(this.flags, offset) == old) {
      this.flags = Conversion.setFlag(this.flags, offset, newValue);
      return true;
    }
    return false;
  }

  public ObjectID setReference(final String fieldName, final ObjectID id) {
    throw new AssertionError("shouldn't be called");
  }

  public void setArrayReference(final int index, final ObjectID id) {
    throw new AssertionError("shouldn't be called");
  }

  public void setValue(final String fieldName, final Object obj) {
    try {
      final TransparentAccess ta = (TransparentAccess) getPeerObject();
      if (ta == null) {
        // Object was GC'd so return which should lead to a re-retrieve
        return;
      }
      clearReference(fieldName);
      final TCField field = getTCClass().getField(fieldName);
      if (field == null) {
        logger.warn("Data for field:" + fieldName + " was recieved but that field does not exist in class:");
        return;
      }
      if (obj instanceof ObjectID) {
        setReference(fieldName, (ObjectID) obj);
        ta.__tc_setfield(field.getName(), null);
      } else {
        // clean this up
        ta.__tc_setfield(field.getName(), obj);
      }
    } catch (final Exception e) {
      logger.error("Error setting field [" + fieldName + "] to value of type " + typeOf(obj) + " on instance of "
                   + getTCClass().getPeerClass().getName() + " that has fields: " + fieldDesc());

      // TODO: More elegant exception handling.
      throw new com.tc.object.dna.api.DNAException(e);
    }
  }

  private String fieldDesc() {
    List<String> fields = new ArrayList<String>();
    Class c = getTCClass().getPeerClass();
    while (c != null) {
      for (Field f : c.getDeclaredFields()) {
        fields.add(c.getName() + "." + f.getName() + "(" + f.getType().getName() + ")");
      }
      c = c.getSuperclass();
    }
    return fields.toString();
  }

  private static String typeOf(Object obj) {
    if (obj == null) { return "null"; }
    return obj.getClass().getSimpleName();
  }

  public final int clearReferences(final int toClear) {
    if (this.tcClazz.useResolveLockWhileClearing()) {
      synchronized (getResolveLock()) {
        return basicClearReferences(toClear);
      }
    } else {
      return basicClearReferences(toClear);
    }
  }

  private int basicClearReferences(final int toClear) {
    final Object po = getPeerObject();
    Assert.assertFalse(isNew()); // Shouldn't clear new Objects
    if (po == null) { return 0; }
    return clearReferences(po, toClear);
  }

  protected abstract int clearReferences(Object pojo, int toClear);

  public final Object getResolveLock() {
    return this.objectID; // Save a field by using this one as the lock
  }

  public void resolveArrayReference(final int index) {
    throw new AssertionError("shouldn't be called");
  }

  public void clearArrayReference(final int index) {
    clearReference(Integer.toString(index));
  }

  public void clearReference(final String fieldName) {
    // do nothing
  }

  public void resolveReference(final String fieldName) {
    // do nothing
  }

  public void resolveAllReferences() {
    // override me
  }

  public void literalValueChanged(final Object newValue, final Object oldValue) {
    throw new UnsupportedOperationException();
  }

  public void setLiteralValue(final Object newValue) {
    throw new UnsupportedOperationException();
  }

  public synchronized void setVersion(final long version) {
    this.version = version;
  }

  public synchronized long getVersion() {
    return this.version;
  }

  @Override
  public String toString() {
    return getClass().getName() + "@" + System.identityHashCode(this) + "[objectID=" + this.objectID + ", TCClass="
           + this.tcClazz + "]";
  }

  public void objectFieldChanged(final String classname, final String fieldname, final Object newValue, final int index) {
    try {
      markAccessed();
      if (index == NULL_INDEX) {
        // Assert.eval(fieldname.indexOf('.') >= 0);
        clearReference(fieldname);
      } else {
        clearArrayReference(index);
      }
      getObjectManager().getTransactionManager().fieldChanged(this, classname, fieldname, newValue, index);
    } catch (final Throwable t) {
      Util.printLogAndRethrowError(t, logger);
    }
  }

  public void objectFieldChangedByOffset(final String classname, final long fieldOffset, final Object newValue,
                                         final int index) {
    throw new AssertionError();
  }

  public boolean isFieldPortableByOffset(final long fieldOffset) {
    return this.tcClazz.isPortableField(fieldOffset);
  }

  public String getFieldNameByOffset(final long fieldOffset) {
    throw new AssertionError();
  }

  public void booleanFieldChanged(final String classname, final String fieldname, final boolean newValue,
                                  final int index) {
    objectFieldChanged(classname, fieldname, Boolean.valueOf(newValue), index);
  }

  public void byteFieldChanged(final String classname, final String fieldname, final byte newValue, final int index) {
    objectFieldChanged(classname, fieldname, Byte.valueOf(newValue), index);
  }

  public void charFieldChanged(final String classname, final String fieldname, final char newValue, final int index) {
    objectFieldChanged(classname, fieldname, Character.valueOf(newValue), index);
  }

  public void doubleFieldChanged(final String classname, final String fieldname, final double newValue, final int index) {
    objectFieldChanged(classname, fieldname, Double.valueOf(newValue), index);
  }

  public void floatFieldChanged(final String classname, final String fieldname, final float newValue, final int index) {
    objectFieldChanged(classname, fieldname, Float.valueOf(newValue), index);
  }

  public void intFieldChanged(final String classname, final String fieldname, final int newValue, final int index) {
    objectFieldChanged(classname, fieldname, Integer.valueOf(newValue), index);
  }

  public void longFieldChanged(final String classname, final String fieldname, final long newValue, final int index) {
    objectFieldChanged(classname, fieldname, Long.valueOf(newValue), index);
  }

  public void shortFieldChanged(final String classname, final String fieldname, final short newValue, final int index) {
    objectFieldChanged(classname, fieldname, Short.valueOf(newValue), index);
  }

  public void objectArrayChanged(final int startPos, final Object[] array, final int length) {
    markAccessed();
    for (int i = 0; i < length; i++) {
      clearArrayReference(startPos + i);
    }
    getObjectManager().getTransactionManager().arrayChanged(this, startPos, array, length);
  }

  public void primitiveArrayChanged(final int startPos, final Object array, final int length) {
    markAccessed();
    getObjectManager().getTransactionManager().arrayChanged(this, startPos, array, length);
  }

  public void setNext(final TLinkable link) {
    throw new UnsupportedOperationException();
  }

  public void setPrevious(final TLinkable link) {
    throw new UnsupportedOperationException();
  }

  public TLinkable getNext() {
    throw new UnsupportedOperationException();
  }

  public TLinkable getPrevious() {
    throw new UnsupportedOperationException();
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

  public int accessCount(final int factor) {
    throw new UnsupportedOperationException();
  }

  public boolean isNew() {
    return getFlag(IS_NEW_OFFSET);
  }

  public void setNotNew() {
    // Flipping the "new" flag must occur AFTER dehydrate -- otherwise the client
    // memory manager might start nulling field values! (see canEvict() dependency on isNew() condition)
    if (!compareAndSetFlag(IS_NEW_OFFSET, true, false)) { throw new AssertionError(this + " : Already not new"); }
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

  public final synchronized boolean canEvict() {
    return isEvictable() && !this.tcClazz.isNotClearable() && !isNew();
  }

  public boolean isCacheManaged() {
    return !this.tcClazz.isNotClearable();
  }

  protected abstract boolean isEvictable();

  public ToggleableStrongReference getOrCreateToggleRef() {
    final Object peer = getPeerObject();
    if (peer == null) { throw new AssertionError("cannot create a toggle reference if peer object is gone"); }

    return getObjectManager().getOrCreateToggleRef(this.objectID, peer);
  }

}
