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
import com.tc.object.dna.api.DNAInternal;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.objectserver.api.EvictableEntry;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public final class ServerMapEvictionDNA implements DNAInternal {

  private final ObjectID oid;
  private final Map<Object, EvictableEntry>      evictionCandidates;
  private final String   className;
  private final String   cacheName;

  public ServerMapEvictionDNA(final ObjectID oid, final String className, final Map<Object, EvictableEntry> candidates, String cacheName) {
    this.oid = oid;
    this.className = className;
    this.evictionCandidates = candidates;
    this.cacheName = cacheName;
  }

  @Override
  public int getArraySize() {
    return 0;
  }

  @Override
  public DNACursor getCursor() {
    return new ServerMapEvictionDNACursor(this.evictionCandidates);
  }

  @Override
  public ObjectID getObjectID() throws DNAException {
    return this.oid;
  }

  @Override
  public ObjectID getParentObjectID() throws DNAException {
    return ObjectID.NULL_ID;
  }

  @Override
  public String getTypeName() {
    return this.className;
  }

  @Override
  public long getVersion() {
    return DNA.NULL_VERSION;
  }

  @Override
  public boolean hasLength() {
    return false;
  }

  @Override
  public boolean isDelta() {
    return true;
  }

  @Override
  public MetaDataReader getMetaDataReader() {
    return new ServerMapEvictionMetaDataReader(oid, cacheName, evictionCandidates);
  }

  @Override
  public boolean hasMetaData() {
    return true;
  }

  private static final class ServerMapEvictionDNACursor implements DNACursor {

    private static final LogicalAction evictionCompleted = new LogicalAction(SerializationUtil.EVICTION_COMPLETED,
                                                                             new Object[] {});

    private final Iterator<Entry<Object, EvictableEntry>>      actions;
    private int                        actionsCount;
    private LogicalAction              currentAction;

    public ServerMapEvictionDNACursor(final Map<Object, EvictableEntry> candidates) {
      this.actions = candidates.entrySet().iterator();
      this.actionsCount = candidates.size() + 1; // plus one for evictionComplete action
    }

    @Override
    public Object getAction() {
      return this.currentAction;
    }

    @Override
    public int getActionCount() {
      return actionsCount;
    }

    @Override
    public LogicalAction getLogicalAction() {
      return this.currentAction;
    }

    @Override
    public PhysicalAction getPhysicalAction() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean next() {
      if (this.actions.hasNext()) {
        final Entry<Object, EvictableEntry> e = this.actions.next();
        this.currentAction = new LogicalAction(SerializationUtil.REMOVE_IF_VALUE_EQUAL, new Object[] { e.getKey(),
            e.getValue().getObjectID() });
        actionsCount--;
        return true;
      } else if (actionsCount == 1) {
        currentAction = evictionCompleted;
        actionsCount--;
        return true;
      } else if (actionsCount > 0) { throw new AssertionError("Expected Actions count to be 0 : " + actionsCount); }
      return false;
    }

    @Override
    public boolean next(final DNAEncoding arg) {
      return next();
    }

    @Override
    public void reset() throws UnsupportedOperationException {
      throw new UnsupportedOperationException("Reset is not supported by this class");
    }
  }
}
