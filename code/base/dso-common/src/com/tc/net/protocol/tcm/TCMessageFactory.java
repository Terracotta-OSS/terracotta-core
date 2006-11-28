/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.bytes.TCByteBuffer;

public interface TCMessageFactory {

  public TCMessage createMessage(MessageChannel source, TCMessageType type);

  public TCMessage createMessage(MessageChannel source, TCMessageType type, TCMessageHeader header, TCByteBuffer[] data);

  public void addClassMapping(TCMessageType type, Class msgClass);

}
