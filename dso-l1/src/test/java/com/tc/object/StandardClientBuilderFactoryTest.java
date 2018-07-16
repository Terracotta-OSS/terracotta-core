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