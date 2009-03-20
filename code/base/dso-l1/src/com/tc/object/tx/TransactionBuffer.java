/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.io.TCByteBufferOutputStream;

interface TransactionBuffer {

  public void writeTo(TCByteBufferOutputStream dest);

  public int write(ClientTransaction txn);

}