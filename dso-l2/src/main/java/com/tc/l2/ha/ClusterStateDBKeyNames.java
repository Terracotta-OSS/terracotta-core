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
package com.tc.l2.ha;

public interface ClusterStateDBKeyNames {
  public static final String DATABASE_CREATION_TIMESTAMP_KEY = "BERKELEYDB::DB_CREATION_TIMESTAMP_KEY";
  public static final String L2_STATE_KEY                    = "CLUSTER_STATE::L2_STATE_KEY";
  public static final String CLUSTER_ID_KEY                  = "CLUSTER_STATE::CLUSTER_ID_KEY";
  public static final String STRIPE_KEY_PREFIX               = "STRIPE_STATE::KEY_PREFIX";
}
