/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.gtx;

public class GlobalTransactionIDAlreadySetException extends Exception {

  public GlobalTransactionIDAlreadySetException(String msg) {
    super(msg);
  }

}
