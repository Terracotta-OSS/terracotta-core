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
package com.tc.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.object.ClientIDProvider;

public class ClientIDLoggerProvider implements TCLoggerProvider {

  private final ClientIDProvider cidProvider;

  public ClientIDLoggerProvider(ClientIDProvider clientIDProvider) {
    this.cidProvider = clientIDProvider;
  }
  
  @Override
  public Logger getLogger(Class<?> clazz) {
    return new ClientIDLogger(cidProvider, LoggerFactory.getLogger(clazz));
  }

  @Override
  public Logger getLogger(String name) {
    return new ClientIDLogger(cidProvider, LoggerFactory.getLogger(name));
  }

}
