/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCDataInput;
import com.tc.io.TCDataOutput;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
public class ObjectStringSerializerImpl implements ObjectStringSerializer {
  private final double            STRING_LEN_TO_UTF8_RATIO = 1.25;                   // ranges from 1-3 bytes per
                                                                                      // character, since we usually use
                                                                                      // this for field names which are
                                                                                      // ascii, it'll tend towards 1.
  private final Map<Object, Integer> stringToID               = new HashMap<Object, Integer>();
  private final Map<Integer, Object> idToString               = new HashMap<Integer, Object>();

  private final Map<Object, Integer> bytesToID                = new HashMap<Object, Integer>();
  private final Map<Integer, Object> idToBytes                = new HashMap<Integer, Object>();

  private int                     approximateBytesWritten             = 0;

  private static class Serialize {
    private final TCDataOutput out;

    public Serialize(TCDataOutput out) {
      this.out = out;
    }

    public boolean writeToTcDataOutput(Object key, int value) {
      this.out.writeString((String) key);
      this.out.writeInt(value);
      return true;
    }


  }

  private static class BytesSerialize {
    private final TCDataOutput out;

    public BytesSerialize(TCDataOutput out) {
      this.out = out;
    }

    public boolean write(Object key, int value) {
      BytesKey bk = (BytesKey) key;
      byte[] b = bk.getBytes();

      this.out.writeInt(b.length);
      this.out.write(b);
      this.out.writeInt(value);
      return true;
    }
  }

  @Override
  public synchronized void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(this.stringToID.size());
    Serialize serializeProcedure =     new Serialize(serialOutput);
    for (Entry<Object, Integer> entry : stringToID.entrySet()) {
      serializeProcedure.writeToTcDataOutput(entry.getKey(), entry.getValue());
    }

    serialOutput.writeInt(this.bytesToID.size());
    BytesSerialize bytesSerializeProcedure = new BytesSerialize(serialOutput);
    for (Entry<Object, Integer> entry : bytesToID.entrySet()) {
      bytesSerializeProcedure.write(entry.getKey(),entry.getValue());
      }
  }

  @Override
  public synchronized ObjectStringSerializer deserializeFrom(TCByteBufferInput serialInput) throws IOException {
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

  @Override
  public synchronized void writeString(TCDataOutput out, String string) {
    int sid = -1;
    if (this.stringToID.containsKey(string)) {
      sid = idForString(string);
    } else {
      sid = createID(string);
    }
    out.writeInt(sid);
  }

  @Override
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

  @Override
  public synchronized void writeFieldName(TCDataOutput out, String fieldName) {
    writeString(out, fieldName);
  }

  @Override
  public synchronized String readString(TCDataInput in) throws IOException {
    final int id = in.readInt();

    final String string = stringForID(id);
    if (string == null) { throw new AssertionError("cid:" + id + " map:" + this.stringToID); }

    return string;
  }

  @Override
  public synchronized byte[] readStringBytes(TCDataInput input) throws IOException {
    final int id = input.readInt();

    byte[] bytes = (byte[]) this.idToBytes.get(id);
    if (bytes == null) {
      //
      throw new AssertionError("missing bytes for id=" + id);
    }

    return bytes;
  }

  @Override
  public synchronized String readFieldName(TCDataInput in) throws IOException {
    final int stringID = in.readInt();

    final String fieldName = stringForID(stringID);
    Assert.eval(fieldName != null);
    return fieldName;
  }

  private void addStringAndID(String name, int id) {
    name = name.intern();
    this.stringToID.put(name, id);
    this.idToString.put(id, name);
  }

  private void addBytesAndID(byte[] b, int id) {
    this.bytesToID.put(new BytesKey(b), id);
    this.idToBytes.put(id, b);
  }

  private String stringForID(int id) {
    return (String) this.idToString.get(id);
  }

  private int idForString(String string) {
    return this.stringToID.get(string);
  }

  private int createID(String string) {
    Assert.assertNotNull(string);
    final int newID = this.stringToID.size() + 1;
    this.stringToID.put(string, newID);
    this.idToString.put(newID, string);
    // Writing out a string as UTF-8 (with dataOuput.writeUTF) results in a length (in a short), and the string in UTF-8
    // see: http://docs.oracle.com/javase/6/docs/api/java/io/DataInput.html#modified-utf-8
    approximateBytesWritten += (Short.SIZE / 8);
    approximateBytesWritten += (string.length() * STRING_LEN_TO_UTF8_RATIO);
    approximateBytesWritten += 2; // Some other bytes we write in TCByteBufferOutputStream.writeString()
    approximateBytesWritten += (Integer.SIZE / 8); // id
    return newID;
  }

  private int createBytesID(BytesKey key) {
    int id = this.bytesToID.size() + 1;
    this.bytesToID.put(key, id);
    this.idToBytes.put(id, key.getBytes());
    approximateBytesWritten += key.getBytes().length;
    approximateBytesWritten += (Integer.SIZE / 8) * 2; // length and id
    return id;
  }

  @Override
  public int getApproximateBytesWritten() {
    return approximateBytesWritten;
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
