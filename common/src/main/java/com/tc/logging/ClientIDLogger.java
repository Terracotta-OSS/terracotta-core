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
package com.tc.logging;

import org.slf4j.Logger;

import com.tc.object.ClientIDProvider;

public class ClientIDLogger extends BaseMessageDecoratorTCLogger {

  private final ClientIDProvider cidp;

  public ClientIDLogger(ClientIDProvider clientIDProvider, Logger logger) {
    super(logger);
    this.cidp = clientIDProvider;
  }

  @Override
  protected String decorate(Object msg) {
    return cidp.getClientID() + ": " + msg;
  }

}
