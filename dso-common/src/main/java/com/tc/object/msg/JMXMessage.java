/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.TCRuntimeException;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCSerializable;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class JMXMessage extends DSOMessageBase implements TCSerializable {

  private static final byte JMX_OBJECT = 0;
  private Object            jmxObject;

  public JMXMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel,
                    TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public JMXMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header,
                    TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(JMX_OBJECT, this);
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case JMX_OBJECT:
        jmxObject = getObject(this);
        return true;
      default:
        return false;
    }
  }

  public Object getJMXObject() {
    return jmxObject;
  }

  public void setJMXObject(Serializable jmxObject) {
    this.jmxObject = jmxObject;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    try {
      ByteArrayOutputStream bao = new ByteArrayOutputStream(1024);
      ObjectOutputStream oos = new ObjectOutputStream(bao);
      oos.writeObject(jmxObject);
      oos.close();

      byte serializedObject[] = bao.toByteArray();
      serialOutput.writeInt(serializedObject.length);
      serialOutput.write(serializedObject);
    } catch (IOException e) {
      e.printStackTrace();
      throw new TCRuntimeException(e);
    }
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    try {
      int length = serialInput.readInt();
      byte serializedObject[] = new byte[length];
      serialInput.read(serializedObject);
      ByteArrayInputStream bis = new ByteArrayInputStream(serializedObject);
      ObjectInputStream ois = new ObjectInputStream(bis);
      return ois.readObject();
    } catch (IOException e) {
      throw e;
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      throw new TCRuntimeException(e);
    }
  }

}
