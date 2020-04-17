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
 */
package com.tc.object;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


import com.terracotta.diagnostic.DiagnosticClientBuilder;

import java.util.Properties;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class StandardClientBuilderFactoryTest {

  private final StandardClientBuilderFactory standardClientBuilderFactory = new StandardClientBuilderFactory();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void createStandardBuilder() throws Exception {
    Properties connectionProperties = new Properties();
    connectionProperties.put(ClientBuilderFactory.CLIENT_BUILDER_TYPE,
                             ClientBuilderFactory.ClientBuilderType.TERRACOTTA);
    ClientBuilder clientBuilder = standardClientBuilderFactory.create(connectionProperties);
    assertThat(clientBuilder, instanceOf(StandardClientBuilder.class));
  }

  @Test
  public void createDiagnosticBuilder() throws Exception {
    Properties connectionProperties = new Properties();
    connectionProperties.put(ClientBuilderFactory.CLIENT_BUILDER_TYPE,
                             ClientBuilderFactory.ClientBuilderType.DIAGNOSTIC);
    ClientBuilder clientBuilder = standardClientBuilderFactory.create(connectionProperties);
    assertThat(clientBuilder, instanceOf(DiagnosticClientBuilder.class));
  }

  @Test
  public void invalidConnectionType() throws Exception {
    Properties connectionProperties = new Properties();
    connectionProperties.put(ClientBuilderFactory.CLIENT_BUILDER_TYPE, "invalid");
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Received invalid value");
    standardClientBuilderFactory.create(connectionProperties);
  }
}