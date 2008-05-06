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

  private final TObjectIntHashMap  stringToID = new TObjectIntHashMap();
  private final TIntObjectHashMap  idToString = new TIntObjectHashMap();

  private static class SerializeProcedure implements TObjectIntProcedure {
    private final TCDataOutput out;

    public SerializeProcedure(TCDataOutput out) {
      this.out = out;
    }

    public boolean execute(Object key, int value) {
      out.writeString((String) key);
      out.writeInt(value);
      return true;
    }
  }

  public synchronized void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(stringToID.size());
    stringToID.forEachEntry(new SerializeProcedure(serialOutput));
  }

  public synchronized Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    int size = serialInput.readInt();
    for (int i = 0; i < size; i++) {
      addStringAndID(serialInput.readString(), serialInput.readInt());
    }
    return this;
  }

  public synchronized void writeString(TCByteBufferOutput out, String string) {
    int sid = -1;
    if (stringToID.containsKey(string)) {
      sid = idForString(string);
    } else {
      sid = createID(string);
    }
    out.writeInt(sid);
  }

  public synchronized void writeFieldName(TCByteBufferOutput out, String fieldName) {
    writeString(out, fieldName);
  }

  public synchronized String readString(TCByteBufferInput in) throws IOException {
    int id = in.readInt();

    String string = stringForID(id);
    if (string == null) { throw new AssertionError("cid:" + id + " map:" + stringToID); }

    return string;
  }

  public synchronized String readFieldName(TCByteBufferInput in) throws IOException {
    int stringID = in.readInt();

    String fieldName = stringForID(stringID);
    Assert.eval(fieldName != null);
    return fieldName;
  }

  private void addStringAndID(String name, int id) {
    name = name.intern();
    stringToID.put(name, id);
    idToString.put(id, name);
  }

  private String stringForID(int id) {
    return (String) idToString.get(id);
  }

  private int idForString(String string) {
    return stringToID.get(string);
  }

  private int createID(String string) {
    Assert.assertNotNull(string);
    int newID = stringToID.size() + 1;
    stringToID.put(string, newID);
    idToString.put(newID, string);
    return newID;
  }
}
