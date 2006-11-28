/*
 * Created on Apr 28, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.WaitContext;

import java.util.Collection;

public interface ClientHandshakeMessage {

  public Collection getTransactionSequenceIDs();

  public void addObjectID(ObjectID object);

  public Collection getObjectIDs();

  public void addLockContext(LockContext ctxt);

  public Collection getLockContexts();

  public void addWaitContext(WaitContext ctxt);

  public Collection getWaitContexts();

  public void addPendingLockContext(LockContext ctxt);

  public Collection getPendingLockContexts();

  public ChannelID getChannelID();

  public void send();

  public void setTransactionSequenceIDs(Collection transactionSequenceIDs);

  public void setResentTransactionIDs(Collection resentTransactionIDs);

  public Collection getResentTransactionIDs();

  public void setIsObjectIDsRequested(boolean request);

  public boolean isObjectIDsRequested();

}
