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
package com.tc.objectserver.tx;

import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
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

/**
 * @author tim
 */
public class RemoveAllDNA implements DNAInternal {
  protected final ObjectID oid;
  protected final Map<Object, EvictableEntry>      evictionCandidates;
  protected final String   cacheName;

  public RemoveAllDNA(final ObjectID oid, String cacheName, final Map<Object, EvictableEntry> candidates) {
    this.oid = oid;
    this.evictionCandidates = candidates;
    this.cacheName = cacheName;
  }

  @Override
  public int getArraySize() {
    return 0;
  }

  @Override
  public DNACursor getCursor() {
    return new RemoveAllDNACursor(this.evictionCandidates);
  }

  @Override
  public ObjectID getObjectID() throws DNAException {
    return oid;
  }

  @Override
  public ObjectID getParentObjectID() throws DNAException {
    return ObjectID.NULL_ID;
  }

  @Override
  public String getTypeName() {
    return null;
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
    return new RemoveAllMetaDataReader(oid, cacheName, evictionCandidates);
  }

  @Override
  public boolean hasMetaData() {
    return true;
  }

  private static class RemoveAllDNACursor implements DNACursor {

    private final Iterator<Map.Entry<Object, EvictableEntry>> actions;
    private int                        actionsCount;
    private LogicalAction              currentAction;

    public RemoveAllDNACursor(final Map<Object, EvictableEntry> candidates) {
      this.actions = candidates.entrySet().iterator();
      this.actionsCount = candidates.size();
    }

    @Override
    public Object getAction() {
      return currentAction;
    }

    @Override
    public int getActionCount() {
      return actionsCount;
    }

    @Override
    public LogicalAction getLogicalAction() {
      return currentAction;
    }

    @Override
    public PhysicalAction getPhysicalAction() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean next() {
      if (actions.hasNext()) {
        final Map.Entry<Object, EvictableEntry> e = actions.next();
        currentAction = new LogicalAction(LogicalOperation.REMOVE_IF_VALUE_EQUAL, new Object[] { e.getKey(),
            e.getValue().getObjectID() });
        actionsCount--;
        return true;
      } else {
        return false;
      }
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
