/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCDataOutput;
import com.tc.io.TCSerializable;
import com.tc.util.Assert;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntProcedure;

import java.io.IOException;

public class ObjectStringSerializer implements TCSerializable {

  private final TObjectIntHashMap stringToID = new TObjectIntHashMap();
  private final TIntObjectHashMap idToString = new TIntObjectHashMap();

  private static class SerializeProcedure implements TObjectIntProcedure {
    private final TCDataOutput out;

    public SerializeProcedure(final TCDataOutput out) {
      this.out = out;
    }

    public boolean execute(final Object key, final int value) {
      this.out.writeString((String) key);
      this.out.writeInt(value);
      return true;
    }
  }

  public synchronized void serializeTo(final TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(this.stringToID.size());
    this.stringToID.forEachEntry(new SerializeProcedure(serialOutput));
  }

  public synchronized Object deserializeFrom(final TCByteBufferInput serialInput) throws IOException {
    final int size = serialInput.readInt();
    for (int i = 0; i < size; i++) {
      addStringAndID(serialInput.readString(), serialInput.readInt());
    }
    return this;
  }

  public synchronized void writeString(final TCByteBufferOutput out, final String string) {
    int sid = -1;
    if (this.stringToID.containsKey(string)) {
      sid = idForString(string);
    } else {
      sid = createID(string);
    }
    out.writeInt(sid);
  }

  public synchronized void writeFieldName(final TCByteBufferOutput out, final String fieldName) {
    writeString(out, fieldName);
  }

  public synchronized String readString(final TCByteBufferInput in) throws IOException {
    final int id = in.readInt();

    final String string = stringForID(id);
    if (string == null) { throw new AssertionError("cid:" + id + " map:" + this.stringToID); }

    return string;
  }

  public synchronized String readFieldName(final TCByteBufferInput in) throws IOException {
    final int stringID = in.readInt();

    final String fieldName = stringForID(stringID);
    Assert.eval(fieldName != null);
    return fieldName;
  }

  private void addStringAndID(String name, final int id) {
    name = name.intern();
    this.stringToID.put(name, id);
    this.idToString.put(id, name);
  }

  private String stringForID(final int id) {
    return (String) this.idToString.get(id);
  }

  private int idForString(final String string) {
    return this.stringToID.get(string);
  }

  private int createID(final String string) {
    Assert.assertNotNull(string);
    final int newID = this.stringToID.size() + 1;
    this.stringToID.put(string, newID);
    this.idToString.put(newID, string);
    return newID;
  }
}
