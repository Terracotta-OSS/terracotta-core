/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.tickertoken;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public abstract class TickerTokenImpl implements TickerToken, TCSerializable {

  protected int primaryID;
  protected int startTick;
  protected int totalTickers;

  public TickerTokenImpl() {
    //
  }

  public TickerTokenImpl(int primaryID, int startTick, int totalTickers) {
    this.primaryID = primaryID;
    this.startTick = startTick;
    this.totalTickers = totalTickers;
  }

  public int getStartTick() {
    return startTick;
  }

  public int getPrimaryID() {
    return primaryID;
  }

  public int getTotalTickers() {
    return totalTickers;
  }

  public abstract void collectToken(int aId, CollectContext context);

  public abstract boolean evaluateComplete();

  public Object deserializeFrom(TCByteBufferInput serialInput) {
    try {
      primaryID = serialInput.readInt();
      startTick = serialInput.readInt();
      totalTickers = serialInput.readInt();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(primaryID);
    serialOutput.writeInt(startTick);
    serialOutput.writeInt(totalTickers);
  }

  protected void serializeObject(TCByteBufferOutput serialOutput, Object obj) {
    ByteArrayOutputStream boas = new ByteArrayOutputStream();
    try {
      ObjectOutputStream oos = new ObjectOutputStream(boas);
      oos.writeObject(obj);
      oos.close();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    byte[] objectArray = boas.toByteArray();
    serialOutput.writeInt(objectArray.length);
    serialOutput.write(objectArray);
  }

  protected Object deserializeObject(TCByteBufferInput serialInput) {
    Object obj = null;
    try {
      int objectArrayLength = serialInput.readInt();
      byte[] objectArray = new byte[objectArrayLength];
      serialInput.read(objectArray);
      ByteArrayInputStream bais = new ByteArrayInputStream(objectArray);
      ObjectInputStream ois = new ObjectInputStream(bais);
      obj = ois.readObject();
    } catch (ClassNotFoundException e) {
      throw new AssertionError(e);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    return obj;
  }

}
