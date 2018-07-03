package com.tc.object;

import org.junit.jupiter.api.Test;

import com.terracotta.diagnostic.DiagnosticClientBuilder;

import java.util.Properties;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StandardClientBuilderFactoryTest {

  private final StandardClientBuilderFactory standardClientBuilderFactory = new StandardClientBuilderFactory();

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

    Throwable t = assertThrows(IllegalArgumentException.class, ()-> {
      standardClientBuilderFactory.create(connectionProperties);
    });

    assertThat(t.getMessage(), containsString("Received invalid value"));
  }
}