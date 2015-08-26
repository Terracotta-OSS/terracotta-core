package org.terracotta.passthrough;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * The top-level abstract class of all messages passed through the inter-thread "wire" between the "server" and "client".
 */
public abstract class PassthroughMessage {
  public long transactionID;
  
  public void setTransactionID(long transactionID) {
    this.transactionID = transactionID;
  }

  public byte[] asSerializedBytes() {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try {
      DataOutputStream output = new DataOutputStream(bytes);
      output.writeLong(this.transactionID);
      this.populateStream(output);
      output.close();
    } catch (IOException e) { 
      // Can't happen with a byte array.
      Assert.unexpected(e);
    }
    return bytes.toByteArray();
  }
  
  protected abstract void populateStream(DataOutputStream output) throws IOException;
}
