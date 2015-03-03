package com.tc.entity;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author twu
 */
public class RequestResponseMessageImpl extends DSOMessageBase implements  RequestResponseMessage {
  private static final byte TRANSACTION_ID = 0;
  private static final byte RESPONSE = 1;
  private static final byte EXCEPTION = 2;
  
  private byte[] response;
  private TransactionID transactionID;
  private Exception exception;
  
  public RequestResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public RequestResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  public void setResponse(TransactionID id, byte[] value) {
    response = value;
    transactionID = id;
  }

  @Override
  public void setResponse(TransactionID id, Exception e) {
    transactionID = id;
    exception = e;
  }

  @Override
  public void setResponse(TransactionID id) {
    transactionID = id;
  }

  public byte[] getResponseValue() {
    return response;
  }

  public TransactionID getTransactionID() {
    return transactionID;
  }

  @Override
  public Exception getException() {
    return exception;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(TRANSACTION_ID, transactionID.toLong());
    if (response != null) {
      putNVPair(RESPONSE, response.length);
      getOutputStream().write(response);
    }
    if (exception != null) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(exception);
      } catch (IOException e) {
        throw new AssertionError(e);
      }
      putNVPair(EXCEPTION, baos.toByteArray().length);
      getOutputStream().write(baos.toByteArray());
    }
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case TRANSACTION_ID:
        transactionID = new TransactionID(getLongValue());
        break;
      case RESPONSE:
        response = getBytesArray();
        break;
      case EXCEPTION:
        ByteArrayInputStream bais = new ByteArrayInputStream(getBytesArray());
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
          exception = (Exception) ois.readObject();
        } catch (ClassNotFoundException e) {
          exception = new Exception("Operation failed but exception class not found.", e);
        }
        break;
      default:
        return false;
    }
    return true;
  }
}
