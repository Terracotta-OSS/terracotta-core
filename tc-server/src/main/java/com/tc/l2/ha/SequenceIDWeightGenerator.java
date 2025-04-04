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

import com.tc.l2.ha.WeightGeneratorFactory.WeightGenerator;
import com.tc.objectserver.handler.ReplicatedTransactionHandler;


public class SequenceIDWeightGenerator implements WeightGenerator {

  private ReplicatedTransactionHandler handler;

  public SequenceIDWeightGenerator() {
  }

  public void setReplicatedTransactionHandler(ReplicatedTransactionHandler handler) {
    this.handler = handler;
  }

  @Override
  public long getWeight() {
    if (handler == null) {
      return -1L;
    }
    return handler.getCurrentSequence();
  }
}
