/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.util.AbstractIdentifier;

/**
 * @author steve To change the template for this generated type comment go to Window&gt;Preferences&gt;Java&gt;Code
 *         Generation&gt;Code and Comments
 */
public class RequestLockContext implements EventContext {
  private final LockID             lockID;
  private final ChannelID          channelID;
  private final AbstractIdentifier sourceID;

  public RequestLockContext(LockID lockID, ChannelID channelID, AbstractIdentifier sourceID) {
    this.lockID = lockID;
    this.channelID = channelID;
    this.sourceID = sourceID;
  }

  public LockID getLockID() {
    return lockID;
  }

  public ChannelID getChannelID() {
    return channelID;
  }

  public AbstractIdentifier getSourceID() {
    return sourceID;
  }

}