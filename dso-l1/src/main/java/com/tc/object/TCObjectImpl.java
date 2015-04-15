/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAWriter;
import com.tc.util.Conversion;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Implementation of TCObject interface.
 */
public abstract class TCObjectImpl implements TCObject {
  private static final TCLogger LOGGER        = TCLogging.getLogger(TCObjectImpl.class);

  private static final int      IS_NEW_OFFSET = 1 << 0;

  // This initial negative version number is important since GID is assigned in the server from 0.
  private long                  version       = -1;

  private final ObjectID        objectID;
  private final TCClass         tcClazz;
  private WeakReference         peerObject;
  private byte                  flags         = 0;

  protected TCObjectImpl(final ObjectID id, final Object peer, final TCClass clazz, final boolean isNew) {
    this.objectID = id;
    this.tcClazz = clazz;
    if (peer != null) {
      setPeerObject(getObjectManager().newWeakObjectReference(id, peer));
    }

    setFlag(IS_NEW_OFFSET, isNew);
  }

  @Override
  public boolean isShared() {
    throw new AssertionError(); // XXX: remove method when possible
  }

  public boolean isNull() {
    return this.peerObject == null || getPeerObject() == null;
  }

  @Override
  public ObjectID getObjectID() {
    return this.objectID;
  }

  protected ClientObjectManager getObjectManager() {
    return getTCClass().getObjectManager();
  }

  @Override
  public Object getPeerObject() {
    return this.peerObject == null ? null : this.peerObject.get();
  }

  protected void setPeerObject(final WeakReference pojo) {
    this.peerObject = pojo;
  }

  protected TCClass getTCClass() {
    return this.tcClazz;
  }

  @Override
  public void dehydrate(final DNAWriter writer) {
    getTCClass().dehydrate(this, writer, getPeerObject());
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
  @Override
  public void hydrate(final DNA from, final boolean force, final WeakReference peer) throws ClassNotFoundException {
    synchronized (getResolveLock()) {
      if (peer != null) {
        setPeerObject(peer);
      }
      final Object po = getPeerObject();
      if (po == null) { return; }
      try {
        getTCClass().hydrate(this, from, po, force);
      } catch (final IOException ioe) {
        LOGGER.warn(ioe);
        throw new DNAException(ioe);
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

  @Override
  public ObjectID setReference(final String fieldName, final ObjectID id) {
    throw new AssertionError("shouldn't be called");
  }

  @Override
  public void setArrayReference(final int index, final ObjectID id) {
    throw new AssertionError("shouldn't be called");
  }

  @Override
  public void setValue(final String fieldName, final Object obj) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public final Object getResolveLock() {
    return this.objectID; // Save a field by using this one as the lock
  }

  @Override
  public void resolveArrayReference(final int index) {
    throw new AssertionError("shouldn't be called");
  }

  @Override
  public void clearReference(final String fieldName) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void resolveReference(final String fieldName) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void resolveAllReferences() {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void literalValueChanged(final Object newValue, final Object oldValue) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void setLiteralValue(final Object newValue) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public synchronized void setVersion(final long version) {
    this.version = version;
  }

  @Override
  public synchronized long getVersion() {
    return this.version;
  }

  @Override
  public String toString() {
    return getClass().getName() + "@" + System.identityHashCode(this) + "[objectID=" + this.objectID + ", TCClass="
           + getTCClass() + "]";
  }

  @Override
  public void objectFieldChanged(final String classname, final String fieldname, final Object newValue, final int index) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void objectFieldChangedByOffset(final String classname, final long fieldOffset, final Object newValue,
                                         final int index) {
    throw new AssertionError();
  }

  @Override
  public String getFieldNameByOffset(final long fieldOffset) {
    throw new AssertionError();
  }

  @Override
  public void booleanFieldChanged(final String classname, final String fieldname, final boolean newValue,
                                  final int index) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void byteFieldChanged(final String classname, final String fieldname, final byte newValue, final int index) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void charFieldChanged(final String classname, final String fieldname, final char newValue, final int index) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void doubleFieldChanged(final String classname, final String fieldname, final double newValue, final int index) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void floatFieldChanged(final String classname, final String fieldname, final float newValue, final int index) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void intFieldChanged(final String classname, final String fieldname, final int newValue, final int index) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void longFieldChanged(final String classname, final String fieldname, final long newValue, final int index) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void shortFieldChanged(final String classname, final String fieldname, final short newValue, final int index) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void objectArrayChanged(final int startPos, final Object[] array, final int length) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void primitiveArrayChanged(final int startPos, final Object array, final int length) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void markAccessed() {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void clearAccessed() {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public boolean recentlyAccessed() {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public int accessCount(final int factor) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public boolean isNew() {
    return getFlag(IS_NEW_OFFSET);
  }

  @Override
  public void setNotNew() {
    // Flipping the "new" flag must occur AFTER dehydrate -- otherwise the client
    // memory manager might start nulling field values! (see canEvict() dependency on isNew() condition)
    if (!compareAndSetFlag(IS_NEW_OFFSET, true, false)) { throw new AssertionError(this + " : Already not new"); }
  }

  // These autlocking disable methods are checked in ManagerImpl. The one known use case
  // is the Hashtable used to hold sessions. We need local synchronization,
  // but we don't ever want autolocks for that particular instance
  @Override
  public void disableAutoLocking() {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public boolean autoLockingDisabled() {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public String getExtendingClassName() {
    return getClassName();
  }

  @Override
  public String getClassName() {
    return getTCClass().getName();
  }

  @Override
  public Class<?> getPeerClass() {
    return getTCClass().getPeerClass();
  }

  @Override
  public boolean isIndexed() {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public boolean isLogical() {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public boolean isEnum() {
    throw new AssertionError(); // XXX: remove method when possible
  }
}
