/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.exception.ImplementMe;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.util.ToggleableStrongReference;
import com.tc.util.concurrent.ThreadUtil;

import java.lang.ref.WeakReference;

import gnu.trove.TLinkable;
import junit.framework.TestCase;

public class ToggleableReferenceManagerTest extends TestCase {

  public void test() throws Exception {
    ToggleableReferenceManager mgr = new ToggleableReferenceManager();
    mgr.start();

    Object peer = new Object();
    TCObject tco = new TCO(1, peer);
    ToggleableStrongReference toggleRef = mgr.getOrCreateFor(tco, peer);
    toggleRef.strongRef();
    peer = null;

    System.gc();
    ThreadUtil.reallySleep(5000);

    Assert.assertEquals(1, mgr.size());
    Assert.assertEquals(0, mgr.clearCount());

    toggleRef.clearStrongRef();
    System.gc();
    ThreadUtil.reallySleep(5000);

    Assert.assertEquals(0, mgr.size());
    Assert.assertEquals(1, mgr.clearCount());
  }

  private static class TCO implements TCObject {

    private final WeakReference peerWeakRef;
    private final ObjectID id;

    public TCO(long id, Object peer) {
       this.id = new ObjectID(id);
       this.peerWeakRef = new WeakReference(peer);
    }

    public boolean autoLockingDisabled() {
      throw new ImplementMe();
    }

    public void booleanFieldChanged(String classname, String fieldname, boolean newValue, int index) {
      throw new ImplementMe();
    }

    public void byteFieldChanged(String classname, String fieldname, byte newValue, int index) {
      throw new ImplementMe();
    }

    public void charFieldChanged(String classname, String fieldname, char newValue, int index) {
      throw new ImplementMe();
    }

    public ArrayIndexOutOfBoundsException checkArrayIndex(int index) {
      throw new ImplementMe();
    }

    public void clearReference(String fieldName) {
      throw new ImplementMe();
    }

    public int clearReferences(int toClear) {
      throw new ImplementMe();
    }

    public boolean dehydrateIfNew(DNAWriter writer) {
      throw new ImplementMe();
    }

    public void disableAutoLocking() {
      throw new ImplementMe();
    }

    public void doubleFieldChanged(String classname, String fieldname, double newValue, int index) {
      throw new ImplementMe();
    }

    public void floatFieldChanged(String classname, String fieldname, float newValue, int index) {
      throw new ImplementMe();
    }

    public String getFieldNameByOffset(long fieldOffset) {
      throw new ImplementMe();
    }

    public TLinkable getNext() {
      throw new ImplementMe();
    }

    public ObjectID getObjectID() {
      return this.id;
    }

    public ToggleableStrongReference getOrCreateToggleRef() {
      throw new ImplementMe();
    }

    public Object getPeerObject() {
      return this.peerWeakRef.get();
    }

    public TLinkable getPrevious() {
      throw new ImplementMe();
    }

    public Object getResolveLock() {
      throw new ImplementMe();
    }

    public TCClass getTCClass() {
      throw new ImplementMe();
    }

    public long getVersion() {
      throw new ImplementMe();
    }

    public void hydrate(DNA from, boolean force) {
      throw new ImplementMe();
    }

    public void intFieldChanged(String classname, String fieldname, int newValue, int index) {
      throw new ImplementMe();
    }

    public boolean isFieldPortableByOffset(long fieldOffset) {
      throw new ImplementMe();
    }

    public boolean isNew() {
      throw new ImplementMe();
    }

    public boolean isShared() {
      throw new ImplementMe();
    }

    public void literalValueChanged(Object newValue, Object oldValue) {
      throw new ImplementMe();
    }

    public void logicalInvoke(int method, String methodSignature, Object[] params) {
      throw new ImplementMe();
    }

    public void longFieldChanged(String classname, String fieldname, long newValue, int index) {
      throw new ImplementMe();
    }

    public void objectArrayChanged(int startPos, Object[] array, int length) {
      throw new ImplementMe();
    }

    public void objectFieldChanged(String classname, String fieldname, Object newValue, int index) {
      throw new ImplementMe();
    }

    public void objectFieldChangedByOffset(String classname, long fieldOffset, Object newValue, int index) {
      throw new ImplementMe();
    }

    public void primitiveArrayChanged(int startPos, Object array, int length) {
      throw new ImplementMe();
    }

    public void resolveAllReferences() {
      throw new ImplementMe();
    }

    public void resolveArrayReference(int index) {
      throw new ImplementMe();
    }

    public void resolveReference(String fieldName) {
      throw new ImplementMe();
    }

    public void setArrayReference(int index, ObjectID id) {
      throw new ImplementMe();
    }

    public void setIsNew() {
      throw new ImplementMe();
    }

    public void setLiteralValue(Object newValue) {
      throw new ImplementMe();
    }

    public void setNext(TLinkable link) {
      throw new ImplementMe();
    }

    public void setPrevious(TLinkable link) {
      throw new ImplementMe();
    }

    public ObjectID setReference(String fieldName, ObjectID id) {
      throw new ImplementMe();
    }

    public void setValue(String fieldName, Object obj) {
      throw new ImplementMe();
    }

    public void setVersion(long version) {
      throw new ImplementMe();
    }

    public void shortFieldChanged(String classname, String fieldname, short newValue, int index) {
      throw new ImplementMe();
    }

    public int accessCount(int factor) {
      throw new ImplementMe();
    }

    public boolean canEvict() {
      throw new ImplementMe();
    }

    public void clearAccessed() {
      throw new ImplementMe();
    }

    public void markAccessed() {
      throw new ImplementMe();
    }

    public boolean recentlyAccessed() {
      throw new ImplementMe();
    }

  }

}
