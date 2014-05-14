/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.tx.ClientTransactionBatchWriter.FoldingConfig;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;

public class FoldingConfigHelper {

  public static FoldingConfig createFromProperties(final TCProperties props) {
    return new FoldingConfig(props.getBoolean(TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_ENABLED),
                             props.getInt(TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_OBJECT_LIMIT),
                             props.getInt(TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_LOCK_LIMIT),
                             props.getBoolean(TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_DEBUG));
  }

}
