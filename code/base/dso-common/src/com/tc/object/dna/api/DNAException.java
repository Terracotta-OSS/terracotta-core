/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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