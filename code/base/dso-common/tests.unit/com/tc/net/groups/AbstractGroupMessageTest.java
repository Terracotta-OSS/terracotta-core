/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCByteBufferTestUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Random;

import junit.framework.TestCase;

public class AbstractGroupMessageTest extends TestCase {

  public void testWriteByteBuffersAndReadByteBuffers() throws Exception {
    TCByteBuffer buffers[] = createRandomBuffers();
    SomeMessage msg = new SomeMessage(buffers);

    // write
    ByteArrayOutputStream bao = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bao);
    oos.writeObject(msg);

    // read
    ByteArrayInputStream bai = new ByteArrayInputStream(bao.toByteArray());
    ObjectInputStream ois = new ObjectInputStream(bai);
    SomeMessage msg1 = (SomeMessage) ois.readObject();

    TCByteBufferTestUtil.checkEquals(msg.getBuffers(), msg1.getBuffers());
  }

  private TCByteBuffer[] createRandomBuffers() {
    Random r = new Random();
    TCByteBuffer[] buffers = new TCByteBuffer[r.nextInt(40)];
    for (int i = 0; i < buffers.length; i++) {
      buffers[i] = TCByteBufferFactory.getInstance(false, r.nextInt(10000));
      r.nextBytes(buffers[i].array());
    }
    return buffers;
  }

  private static class SomeMessage extends AbstractGroupMessage {

    private TCByteBuffer[] buffers;

    public SomeMessage() {
      super(0);
    }

    public SomeMessage(TCByteBuffer[] buffers) {
      this();
      this.buffers = buffers;
    }

    protected void basicReadExternal(int msgType, ObjectInput in) throws IOException {
      this.buffers = readByteBuffers(in);
    }

    protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
      writeByteBuffers(out, buffers);
    }

    public TCByteBuffer[] getBuffers() {
      return buffers;
    }
  }
}
