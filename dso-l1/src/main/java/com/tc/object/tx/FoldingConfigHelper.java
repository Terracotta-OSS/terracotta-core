/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
