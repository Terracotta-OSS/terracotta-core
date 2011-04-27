/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.io.serializer.DSOSerializerPolicy;
import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.io.serializer.api.BasicSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;

public class TCCollectionsSerializerImpl implements TCCollectionsSerializer {

  private final BasicSerializer                   serializer;

  private final ThreadLocal<OutputStreamWrappers> td = new ThreadLocal<OutputStreamWrappers>() {
                                                       @Override
                                                       protected OutputStreamWrappers initialValue() {
                                                         return new OutputStreamWrappers();
                                                       }
                                                     };

  public TCCollectionsSerializerImpl() {
    final DSOSerializerPolicy policy = new DSOSerializerPolicy();
    this.serializer = new BasicSerializer(policy);
  }

  public Object deserialize(final byte[] data) throws IOException, ClassNotFoundException {
    return deserialize(0, data);
  }

  public Object deserialize(final int start, final byte[] data) throws IOException, ClassNotFoundException {
    if (start >= data.length) { return null; }
    final ByteArrayInputStream bai = new ByteArrayInputStream(data, start, data.length - start);
    final ObjectInput ois = new TCObjectInputStream(bai);
    return this.serializer.deserializeFrom(ois);
  }

  public byte[] serialize(final long id, final Object o) throws IOException {
    OutputStreamWrappers outputStreamWrappers = this.td.get();
    TCObjectOutputStream oo = outputStreamWrappers.getTCObjectOutputStream();
    ByteArrayOutputStream bao = outputStreamWrappers.getByteArrayOutputStream();

    oo.writeLong(id);
    this.serializer.serializeTo(o, oo);
    oo.flush();
    final byte b[] = bao.toByteArray();
    bao.reset();
    return b;
  }

  public byte[] serialize(final Object o) throws IOException {
    OutputStreamWrappers outputStreamWrappers = this.td.get();
    TCObjectOutputStream oo = outputStreamWrappers.getTCObjectOutputStream();
    ByteArrayOutputStream bao = outputStreamWrappers.getByteArrayOutputStream();

    this.serializer.serializeTo(o, oo);
    oo.flush();
    final byte b[] = bao.toByteArray();
    bao.reset();
    return b;
  }

  private final static class OutputStreamWrappers {
    private final ByteArrayOutputStream bao;
    private final TCObjectOutputStream  oo;

    private OutputStreamWrappers() {
      this.bao = new ByteArrayOutputStream(1024);
      this.oo = new TCObjectOutputStream(this.bao);
    }

    public ByteArrayOutputStream getByteArrayOutputStream() {
      return bao;
    }

    public TCObjectOutputStream getTCObjectOutputStream() {
      return oo;
    }
  }
}
