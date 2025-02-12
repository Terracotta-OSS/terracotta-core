/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.l2.ha;

public interface ClusterStateDBKeyNames {
  public static final String DATABASE_CREATION_TIMESTAMP_KEY = "BERKELEYDB::DB_CREATION_TIMESTAMP_KEY";
  public static final String L2_STATE_KEY                    = "CLUSTER_STATE::L2_STATE_KEY";
  public static final String CLUSTER_ID_KEY                  = "CLUSTER_STATE::CLUSTER_ID_KEY";
  public static final String STRIPE_KEY_PREFIX               = "STRIPE_STATE::KEY_PREFIX";
}
