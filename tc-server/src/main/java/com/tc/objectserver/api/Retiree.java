/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.objectserver.api;

import com.tc.object.tx.TransactionID;
import java.util.concurrent.CompletionStage;

/**
 * Interface for active entities to communicate to a client that a Entity Request can 
 * be retired of the client.  This interface needs to be associated with a request from a 
 * client
 * 
 * @author 
 */
public interface Retiree {
  CompletionStage<Void> retired(); 
  TransactionID getTransaction();
  String getTraceID();
}
