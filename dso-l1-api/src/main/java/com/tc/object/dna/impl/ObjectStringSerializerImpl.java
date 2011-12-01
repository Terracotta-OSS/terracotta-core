/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCDataInput;
import com.tc.io.TCDataOutput;
import com.tc.util.Assert;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntProcedure;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

public class ObjectStringSerializerImpl implements ObjectStringSerializer {
  private static final Charset    UTF8_CHARSET = Charset.forName("UTF-8");

  private final TObjectIntHashMap stringToID   = new TObjectIntHashMap();
  private final TIntObjectHashMap idToString   = new TIntObjectHashMap();

  private final TObjectIntHashMap bytesToID    = new TObjectIntHashMap();
  private final TIntObjectHashMap idToBytes    = new TIntObjectHashMap();

  private int                     bytesWritten = 0;

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

  private static class BytesSerializeProcedure implements TObjectIntProcedure {
    private final TCDataOutput out;

    public BytesSerializeProcedure(final TCDataOutput out) {
      this.out = out;
    }

    public boolean execute(final Object key, final int value) {
      BytesKey bk = (BytesKey) key;
      byte[] b = bk.getBytes();

      this.out.writeInt(b.length);
      this.out.write(b);
      this.out.writeInt(value);
      return true;
    }
  }

  public synchronized void serializeTo(final TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(this.stringToID.size());
    this.stringToID.forEachEntry(new SerializeProcedure(serialOutput));

    serialOutput.writeInt(this.bytesToID.size());
    this.bytesToID.forEachEntry(new BytesSerializeProcedure(serialOutput));
  }

  public synchronized Object deserializeFrom(final TCByteBufferInput serialInput) throws IOException {
    final int size = serialInput.readInt();
    for (int i = 0; i < size; i++) {
      addStringAndID(serialInput.readString(), serialInput.readInt());
    }

    final int encodingSize = serialInput.readInt();
    for (int i = 0; i < encodingSize; i++) {
      int len = serialInput.readInt();
      byte[] b = new byte[len];
      serialInput.readFully(b);
      addBytesAndID(b, serialInput.readInt());
    }

    return this;
  }

  public synchronized void writeString(final TCDataOutput out, final String string) {
    int sid = -1;
    if (this.stringToID.containsKey(string)) {
      sid = idForString(string);
    } else {
      sid = createID(string);
    }
    out.writeInt(sid);
  }

  public synchronized void writeStringBytes(TCDataOutput out, byte[] bytes) {
    final int id;
    final BytesKey key = new BytesKey(bytes);

    if (this.bytesToID.containsKey(key)) {
      id = bytesToID.get(key);
    } else {
      id = createBytesID(key);
    }

    out.writeInt(id);
  }

  public synchronized void writeFieldName(final TCDataOutput out, final String fieldName) {
    writeString(out, fieldName);
  }

  public synchronized String readString(final TCDataInput in) throws IOException {
    final int id = in.readInt();

    final String string = stringForID(id);
    if (string == null) { throw new AssertionError("cid:" + id + " map:" + this.stringToID); }

    return string;
  }

  public synchronized byte[] readStringBytes(TCDataInput input) throws IOException {
    final int id = input.readInt();

    byte[] bytes = (byte[]) this.idToBytes.get(id);
    if (bytes == null) {
      //
      throw new AssertionError("missing bytes for id=" + id);
    }

    return bytes;
  }

  public synchronized String readFieldName(final TCDataInput in) throws IOException {
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

  private void addBytesAndID(byte[] b, int id) {
    this.bytesToID.put(new BytesKey(b), id);
    this.idToBytes.put(id, b);
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
    // Writing out a string as UTF-8 (with dataOuput.writeUTF) results in a length (in a short), and the string in UTF-8
    // see: http://docs.oracle.com/javase/6/docs/api/java/io/DataInput.html#modified-utf-8
    bytesWritten += (Short.SIZE / 8);
    bytesWritten += string.getBytes(UTF8_CHARSET).length;
    bytesWritten += 2; // Some other bytes we write in TCByteBufferOutputStream.writeString()
    bytesWritten += (Integer.SIZE / 8); // id
    return newID;
  }

  private int createBytesID(BytesKey key) {
    int id = this.bytesToID.size() + 1;
    this.bytesToID.put(key, id);
    this.idToBytes.put(id, key.getBytes());
    bytesWritten += key.getBytes().length;
    bytesWritten += (Integer.SIZE / 8) * 2; // length and id
    return id;
  }

  public int getBytesWritten() {
    return bytesWritten;
  }

  private static class BytesKey {
    private final byte[] b;

    BytesKey(byte[] b) {
      this.b = b;
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(b);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof BytesKey) {
        BytesKey other = (BytesKey) obj;
        return Arrays.equals(b, other.b);
      }
      return false;
    }

    byte[] getBytes() {
      return b;
    }

  }

}
