/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.util.AbstractIdentifier;

/**
 * @author steve
 */
public class TransactionID extends AbstractIdentifier {
  public final static TransactionID NULL_ID             = new TransactionID();
  public final static TransactionID META_DATA_IGNORE_ID = new MetaDataIgnoreTransactionID();

  public TransactionID(long id) {
    super(id);
  }

  private TransactionID() {
    super();
  }

  @Override
  public String getIdentifierType() {
    return "TransactionID";
  }

  public TransactionID next() {
    return new TransactionID(toLong() + 1);
  }

  private static class MetaDataIgnoreTransactionID extends TransactionID {
    // make this a separate subclass so it can never be equal() to a "real" transactionID

    @Override
    public boolean equals(Object obj) {
      return obj == this;
    }
  }

}