/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
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
