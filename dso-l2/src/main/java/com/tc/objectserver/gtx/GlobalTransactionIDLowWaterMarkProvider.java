/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.gtx;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.gtx.GlobalTransactionManager;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionListener;

public class GlobalTransactionIDLowWaterMarkProvider implements GlobalTransactionManager {

  private static final TCLogger             logger              = TCLogging
                                                                    .getLogger(GlobalTransactionIDLowWaterMarkProvider.class);

  private final ServerTransactionManager    transactionManager;
  private final GlobalTransactionManager    gtxm;

  private volatile GlobalTransactionManager lwmProvider;

  private final GlobalTransactionManager    NULL_GLOBAL_TXN_MGR = new GlobalTransactionManager() {
                                                                  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
                                                                    return GlobalTransactionID.NULL_ID;
                                                                  }
                                                                };

  public GlobalTransactionIDLowWaterMarkProvider(ServerTransactionManager transactionManager,
                                                 GlobalTransactionManager gtxm) {
    this.transactionManager = transactionManager;
    this.gtxm = gtxm;
    this.lwmProvider = NULL_GLOBAL_TXN_MGR;
  }

  public void goToActiveMode() {
    transactionManager.callBackOnResentTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
      public void onCompletion() {
        switchLWMProvider();
      }
    });
  }

  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    return lwmProvider.getLowGlobalTransactionIDWatermark();
  }

  private void switchLWMProvider() {
    logger.info("Switching GlobalTransactionID Low Water mark provider since all resent transactions are applied");
    this.lwmProvider = gtxm;
  }
}
