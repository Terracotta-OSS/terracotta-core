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

import com.tc.object.ObjectID;
import com.tc.object.LogicalOperation;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.objectserver.api.EvictableEntry;

import java.io.IOException;
import java.util.Map;

public final class ServerMapEvictionDNA extends RemoveAllDNA {

  public ServerMapEvictionDNA(final ObjectID oid, final Map<Object, EvictableEntry> candidates, String cacheName) {
    super(oid, cacheName, candidates);
  }

  @Override
  public DNACursor getCursor() {
    return new ServerMapEvictionDNACursor(super.getCursor());
  }

  private static class ServerMapEvictionDNACursor implements DNACursor {
    private static final LogicalAction EVICTION_COMPLETED = new LogicalAction(LogicalOperation.EVICTION_COMPLETED, new Object[] {});

    private final DNACursor removeCursor;
    private boolean returnedEvictionCompleted = false;

    private ServerMapEvictionDNACursor(final DNACursor removeCursor) {
      this.removeCursor = removeCursor;
    }

    @Override
    public int getActionCount() {
      return returnedEvictionCompleted ? 0 : removeCursor.getActionCount() + 1;
    }

    @Override
    public boolean next() throws IOException {
      if (removeCursor.next()) {
        return true;
      } else if (!returnedEvictionCompleted) {
        returnedEvictionCompleted = true;
        return true;
      } else {
        return false;
      }
    }

    @Override
    public boolean next(final DNAEncoding encoding) throws IOException, ClassNotFoundException {
      return next();
    }

    @Override
    public void reset() throws UnsupportedOperationException {
      throw new UnsupportedOperationException("Resetting is not supported.");
    }

    @Override
    public LogicalAction getLogicalAction() {
      if (returnedEvictionCompleted) {
        return EVICTION_COMPLETED;
      } else {
        return removeCursor.getLogicalAction();
      }
    }

    @Override
    public PhysicalAction getPhysicalAction() {
      throw new UnsupportedOperationException("No physical action to get.");
    }

    @Override
    public Object getAction() {
      return getLogicalAction();
    }
  }
}
