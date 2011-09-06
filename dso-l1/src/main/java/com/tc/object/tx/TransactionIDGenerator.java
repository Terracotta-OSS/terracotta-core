/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import java.util.concurrent.atomic.AtomicLong;

public class TransactionIDGenerator {

  private AtomicLong id = new AtomicLong(0);

  public TransactionID nextTransactionID() {
    return new TransactionID(id.incrementAndGet());
  }

}
