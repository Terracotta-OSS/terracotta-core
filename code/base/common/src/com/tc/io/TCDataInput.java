/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.io;

import java.io.DataInput;
import java.io.IOException;

public interface TCDataInput extends DataInput {

  public String readString() throws IOException;

  public int read(byte[] b, int off, int len) throws IOException;

}