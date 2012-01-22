/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VersionizedDNAWrapper implements DNA {

  private final long    version;
  private final DNA     dna;
  private final boolean resetSupported;

  public VersionizedDNAWrapper(DNA dna, long version) {
    this(dna, version, false);
  }

  public VersionizedDNAWrapper(DNA dna, long version, boolean resetSupported) {
    this.dna = dna;
    this.version = version;
    this.resetSupported = resetSupported;
  }

  public long getVersion() {
    return version;
  }

  public boolean hasLength() {
    return dna.hasLength();
  }

  public int getArraySize() {
    return dna.getArraySize();
  }

  public String getTypeName() {
    return dna.getTypeName();
  }

  public ObjectID getObjectID() throws DNAException {
    return dna.getObjectID();
  }

  public ObjectID getParentObjectID() throws DNAException {
    return dna.getParentObjectID();
  }

  public DNACursor getCursor() {
    return (resetSupported ? new ResetableDNACursor(dna.getCursor()) : dna.getCursor());
  }

  public String getDefiningLoaderDescription() {
    return dna.getDefiningLoaderDescription();
  }

  public boolean isDelta() {
    return dna.isDelta();
  }

  public String toString() {
    return dna.toString();
  }

  private static class ResetableDNACursor implements DNACursor {

    private final DNACursor cursor;
    private final List      actions = new ArrayList();
    private int             index   = -1;

    public ResetableDNACursor(DNACursor cursor) {
      this.cursor = cursor;
    }

    public int getActionCount() {
      return cursor.getActionCount();
    }

    public boolean next() throws IOException {
      if(++index < actions.size()) {
        return true;
      }
      boolean success = cursor.next();
      if (success) {
        actions.add(cursor.getAction());
      }
      return success;
    }

    public boolean next(DNAEncoding encoding) throws IOException, ClassNotFoundException {
      if(++index < actions.size()) {
        return true;
      }
      boolean success = cursor.next(encoding);
      if (success) {
        actions.add(cursor.getAction());
      }
      return success;
    }

    public void reset() throws UnsupportedOperationException {
      index = -1;
    }

    public LogicalAction getLogicalAction() {
      return (index < actions.size() ? (LogicalAction) actions.get(index) : cursor.getLogicalAction());
    }

    public PhysicalAction getPhysicalAction() {
      return (index < actions.size() ? (PhysicalAction) actions.get(index) : cursor.getPhysicalAction());
    }

    public Object getAction() {
      return (index < actions.size() ? actions.get(index) : cursor.getAction());
    }

    public String toString() {
      return cursor.toString();
    }

  }
}
