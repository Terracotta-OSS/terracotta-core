/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public final class ServerMapEvictionDNA implements DNA {

  private final ObjectID oid;
  private final Map      evictionCandidates;
  private final String   className;
  private final String   loaderDesc;

  public ServerMapEvictionDNA(final ObjectID oid, final String className, final String loaderDesc, final Map candidates) {
    this.oid = oid;
    this.className = className;
    this.loaderDesc = loaderDesc;
    this.evictionCandidates = candidates;
  }

  public int getArraySize() {
    return 0;
  }

  public DNACursor getCursor() {
    return new ServerMapEvictionDNACursor(this.evictionCandidates);
  }

  public String getDefiningLoaderDescription() {
    return this.loaderDesc;
  }

  public ObjectID getObjectID() throws DNAException {
    return this.oid;
  }

  public ObjectID getParentObjectID() throws DNAException {
    return ObjectID.NULL_ID;
  }

  public String getTypeName() {
    return this.className;
  }

  public long getVersion() {
    return DNA.NULL_VERSION;
  }

  public boolean hasLength() {
    return false;
  }

  public boolean isDelta() {
    return true;
  }

  private static final class ServerMapEvictionDNACursor implements DNACursor {

    private final Iterator<Entry> actions;
    private final int             actionsCompleted = 0;
    private LogicalAction         currentAction;
    private final Map             candidates;

    public ServerMapEvictionDNACursor(final Map candidates) {
      this.candidates = candidates;
      this.actions = candidates.entrySet().iterator();
    }

    public Object getAction() {
      return this.currentAction;
    }

    public int getActionCount() {
      return this.candidates.size() - this.actionsCompleted;
    }

    public LogicalAction getLogicalAction() {
      return this.currentAction;
    }

    public PhysicalAction getPhysicalAction() {
      throw new UnsupportedOperationException();
    }

    public boolean next() {
      if (this.actions.hasNext()) {
        final Entry e = this.actions.next();
        this.currentAction = new LogicalAction(SerializationUtil.REMOVE_IF_VALUE_EQUAL, new Object[] { e.getKey(),
            e.getValue() });
        return true;
      }
      return false;
    }

    public boolean next(final DNAEncoding arg) {
      return next();
    }

    public void reset() throws UnsupportedOperationException {
      throw new UnsupportedOperationException("Reset is not supported by this class");
    }
  }
}