/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.util.AbstractIdentifier;

/**
 * @author steve
 */
public class TransactionID extends AbstractIdentifier {
  public final static TransactionID NULL_ID = new TransactionID();
  
  public TransactionID(long id) {
    super(id);
  }

  private TransactionID() {
    super();
  }

  public String getIdentifierType() {
    return "TransactionID";
  }
}