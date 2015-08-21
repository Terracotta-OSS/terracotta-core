/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.gtx;

public class GlobalTransactionIDAlreadySetException extends Exception {

  public GlobalTransactionIDAlreadySetException(String msg) {
    super(msg);
  }

}
