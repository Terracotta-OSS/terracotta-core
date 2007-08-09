/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.dna.api;

import com.tc.exception.TCRuntimeException;

public class DNAException extends TCRuntimeException {
  public DNAException() {
    super();
  }

  public DNAException(String msg) {
    super(msg);
  }

  public DNAException(Exception e) {
    super(e);
  }
}