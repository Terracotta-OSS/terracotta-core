/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.exception.TCClassNotFoundException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.ClientIDProvider;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.event.DmiEventContext;
import com.tc.object.event.DmiManager;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.msg.AcknowledgeTransactionMessage;
import com.tc.object.msg.AcknowledgeTransactionMessageFactory;
import com.tc.object.msg.BroadcastTransactionMessageImpl;
import com.tc.object.session.SessionManager;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.util.Assert;
import com.tcclient.object.DistributedMethodCall;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author steve
 */
public class ReceiveTransactionHandler extends AbstractEventHandler {
  private static final TCLogger                      logger = TCLogging.getLogger(ReceiveTransactionHandler.class);

  private ClientTransactionManager                   txManager;
  private ClientLockManager                          lockManager;
  private final SessionManager                       sessionManager;
  private final ClientGlobalTransactionManager       gtxManager;
  private final AcknowledgeTransactionMessageFactory atmFactory;
  private final ClientIDProvider                     cidProvider;
  private final Sink                                 dmiSink;
  private final DmiManager                           dmiManager;

  public ReceiveTransactionHandler(ClientIDProvider provider, AcknowledgeTransactionMessageFactory atmFactory,
                                   ClientGlobalTransactionManager gtxManager, SessionManager sessionManager,
                                   Sink dmiSink, DmiManager dmiManager) {
    this.cidProvider = provider;
    this.atmFactory = atmFactory;
    this.gtxManager = gtxManager;
    this.sessionManager = sessionManager;
    this.dmiSink = dmiSink;
    this.dmiManager = dmiManager;
  }

  @Override
  public void handleEvent(EventContext context) {
    final BroadcastTransactionMessageImpl btm = (BroadcastTransactionMessageImpl) context;

    if (false) {
      System.err.println(this.cidProvider.getClientID() + ": ReceiveTransactionHandler: committer="
                         + btm.getCommitterID() + ", " + btm.getTransactionID() + btm.getGlobalTransactionID()
                         + ", notified: " + btm.addNotifiesTo(new LinkedList()));
    }

    Assert.eval(btm.getLockIDs().size() > 0);
    GlobalTransactionID lowWaterMark = btm.getLowGlobalTransactionIDWatermark();
    if (!lowWaterMark.isNull()) {
      this.gtxManager.setLowWatermark(lowWaterMark, btm.getSourceNodeID());
    }
    if (this.gtxManager.startApply(btm.getCommitterID(), btm.getTransactionID(), btm.getGlobalTransactionID(), btm
        .getSourceNodeID())) {
      Collection changes = btm.getObjectChanges();
      if (changes.size() > 0 || btm.getNewRoots().size() > 0) {

        if (false) {
          System.err.println(this.cidProvider.getClientID() + " Applying - committer=" + btm.getCommitterID() + " , "
                             + btm.getTransactionID() + " , " + btm.getGlobalTransactionID());
        }

        try {
          this.txManager.apply(btm.getTransactionType(), btm.getLockIDs(), changes, btm.getNewRoots());
        } catch (TCClassNotFoundException cnfe) {
          logger.warn("transaction apply failed for " + btm.getTransactionID(), cnfe);
          // Do not ignore, re-throw to kill this L1
          throw cnfe;
        }

      }
    }

    Collection notifies = btm.addNotifiesTo(new LinkedList());
    for (Iterator i = notifies.iterator(); i.hasNext();) {
      ClientServerExchangeLockContext lc = (ClientServerExchangeLockContext) i.next();
      this.lockManager.notified(lc.getLockID(), lc.getThreadID());
    }

    List dmis = btm.getDmiDescriptors();
    for (Iterator i = dmis.iterator(); i.hasNext();) {
      DmiDescriptor dd = (DmiDescriptor) i.next();

      // NOTE: This prepare call must happen before handing off the DMI to the stage, and more
      // importantly before sending ACK below
      DistributedMethodCall dmc = this.dmiManager.extract(dd);
      if (dmc != null) {
        this.dmiSink.add(new DmiEventContext(dmc));
      }
    }

    // XXX:: This is a potential race condition here 'coz after we decide to send an ACK
    // and before we actually send it, the server may go down and come back up !
    if (this.sessionManager.isCurrentSession(btm.getSourceNodeID(), btm.getLocalSessionID())) {
      AcknowledgeTransactionMessage ack = this.atmFactory.newAcknowledgeTransactionMessage(btm.getSourceNodeID());
      ack.initialize(btm.getCommitterID(), btm.getTransactionID());
      ack.send();
    }
    btm.recycle();
  }

  @Override
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext ccc = (ClientConfigurationContext) context;
    this.txManager = ccc.getTransactionManager();
    this.lockManager = ccc.getLockManager();
  }

}