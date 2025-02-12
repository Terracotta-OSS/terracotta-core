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
package com.tc.object;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


import com.terracotta.diagnostic.DiagnosticClientBuilder;

import java.util.Properties;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import org.terracotta.connection.ConnectionPropertyNames;

public class StandardClientBuilderFactoryTest {

  private final StandardClientBuilderFactory standardClientBuilderFactory = new StandardClientBuilderFactory("terracotta");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void createStandardBuilder() throws Exception {
    Properties connectionProperties = new Properties();
    connectionProperties.put(ConnectionPropertyNames.CONNECTION_TYPE, "terracotta");
    ClientBuilder clientBuilder = standardClientBuilderFactory.create(connectionProperties);
    assertThat(clientBuilder, instanceOf(StandardClientBuilder.class));
  }

  @Test
  public void createDiagnosticBuilder() throws Exception {
    Properties connectionProperties = new Properties();
    connectionProperties.put(ConnectionPropertyNames.CONNECTION_TYPE, "diagnostic");
    ClientBuilder clientBuilder = standardClientBuilderFactory.create(connectionProperties);
    assertThat(clientBuilder, instanceOf(DiagnosticClientBuilder.class));
  }

  @Test
  public void invalidConnectionType() throws Exception {
    Properties connectionProperties = new Properties();
    connectionProperties.put(ConnectionPropertyNames.CONNECTION_TYPE, "invalid");
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("invalid is not a valid connection type");
    standardClientBuilderFactory.create(connectionProperties);
  }
}