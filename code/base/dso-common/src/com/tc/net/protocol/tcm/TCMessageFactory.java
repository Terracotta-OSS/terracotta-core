/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.bytes.TCByteBuffer;

public interface TCMessageFactory {

  public TCMessage createMessage(MessageChannel source, TCMessageType type);

  public TCMessage createMessage(MessageChannel source, TCMessageType type, TCMessageHeader header, TCByteBuffer[] data);

  public void addClassMapping(TCMessageType type, Class msgClass);

  public void addClassMapping(TCMessageType type, GeneratedMessageFactory messageFactory);

}
