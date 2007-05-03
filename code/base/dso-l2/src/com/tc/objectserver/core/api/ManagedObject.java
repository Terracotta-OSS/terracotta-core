/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.impl.ManagedObjectReference;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.managedobject.ManagedObjectTraverser;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

import java.util.Set;

/**
 * @author steve
 */
public interface ManagedObject {

  public ObjectID getID();

  public ManagedObjectReference getReference();

  public Set getObjectReferences();

  public void apply(DNA dna, TransactionID txnID, BackReferences includeIDs, ObjectInstanceMonitor instanceMonitor, boolean ignoreIfOlderDNA)
      throws DNAException;

  public void toDNA(TCByteBufferOutputStream out, ObjectStringSerializer serializer);

  public boolean isDirty();

  public void setIsDirty(boolean isDirty);

  public ManagedObjectFacade createFacade(int limit);

  public boolean isNew();

  public ManagedObjectState getManagedObjectState();

  public void addObjectReferencesTo(ManagedObjectTraverser traverser);

  public long getVersion();
}