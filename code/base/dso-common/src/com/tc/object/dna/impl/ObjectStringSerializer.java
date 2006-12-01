/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCDataOutput;
import com.tc.io.TCSerializable;
import com.tc.util.Assert;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntProcedure;

import java.io.IOException;

public class ObjectStringSerializer implements TCSerializable {

  private final TObjectIntHashMap  stringToID = new TObjectIntHashMap();
  private final TIntObjectHashMap  idToString = new TIntObjectHashMap();
  private final TLongObjectHashMap fields     = new TLongObjectHashMap();

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

  public synchronized Object deserializeFrom(TCByteBufferInputStream serialInput) throws IOException {
    int size = serialInput.readInt();
    for (int i = 0; i < size; i++) {
      addStringAndID(serialInput.readString(), serialInput.readInt());
    }
    return this;
  }

  public synchronized void writeString(TCByteBufferOutputStream out, String string) {
    int sid = -1;
    if (stringToID.containsKey(string)) {
      sid = idForString(string);
    } else {
      sid = createID(string);
    }
    out.writeInt(sid);
  }

  public synchronized void writeFieldName(TCByteBufferOutputStream out, String fieldName) {
    int lastDot = fieldName.lastIndexOf('.');
    String cn = fieldName.substring(0, lastDot);
    String fn = fieldName.substring(lastDot + 1, fieldName.length());

    writeString(out, cn);
    writeString(out, fn);
  }

  public synchronized String readString(TCByteBufferInputStream in) throws IOException {
    int id = in.readInt();

    String string = stringForID(id);
    if (string == null) { throw new AssertionError("cid:" + id + " map:" + stringToID); }

    return string;
  }

  public synchronized String readFieldName(TCByteBufferInputStream in) throws IOException {
    final int classId;
    classId = in.readInt();

    final int fieldId;
    fieldId = in.readInt();

    long key = ((long) classId << 32) + fieldId;
    String rv = (String) fields.get(key);
    if (rv == null) {
      String cn = stringForID(classId);
      Assert.eval(cn != null);
      String fn = stringForID(fieldId);
      Assert.eval(fn != null);
      StringBuffer buf = new StringBuffer(cn.length() + fn.length() + 1); // +1 for '.'
      buf.append(cn).append('.').append(fn);
      rv = buf.toString().intern();
      fields.put(key, rv);
    }

    return rv;
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
