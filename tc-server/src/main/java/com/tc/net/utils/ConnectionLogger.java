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
package com.tc.net.utils;

import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.objectserver.core.impl.GuardianContext;
import com.tc.spi.Guardian;
import java.util.Map;
import java.util.Properties;

/**
 *
 */
public class ConnectionLogger implements TCConnectionEventListener {

  private final String name;

  public ConnectionLogger(String name) {
    this.name = name;
  }

  @Override
  public void connectEvent(TCConnectionEvent event) {
    GuardianContext.validate(Guardian.Op.SECURITY_OP, name + " connection open event", connectionProperties(event));
  }

  @Override
  public void closeEvent(TCConnectionEvent event) {
    GuardianContext.validate(Guardian.Op.SECURITY_OP, name + " connection close event", connectionProperties(event));
  }

  @Override
  public void errorEvent(TCConnectionErrorEvent errorEvent) {
    Properties props = connectionProperties(errorEvent);
    props.setProperty("error", errorEvent.getException().getMessage());
    props.setProperty("messageContext", errorEvent.getMessageContext().toString());
    GuardianContext.validate(Guardian.Op.SECURITY_OP, name + " connection error event", props);
  }

  @Override
  public void endOfFileEvent(TCConnectionEvent event) {
    GuardianContext.validate(Guardian.Op.SECURITY_OP, name + " connection eof event", connectionProperties(event));
  }
  
  private Properties connectionProperties(TCConnectionEvent event) {
    Map<String, ?> state = event.getSource().getState();
    Properties props = new Properties();
    for (String key : state.keySet()) {
      props.setProperty(key, String.valueOf(state.get(key)));
    }
    return props;
  }
}
