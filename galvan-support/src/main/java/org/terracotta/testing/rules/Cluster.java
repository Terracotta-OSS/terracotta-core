/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.testing.rules;

import java.net.URI;
import org.junit.rules.ExternalResource;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.passthrough.IClusterControl;

/**
 *
 * @author cdennis
 */
public abstract class Cluster extends ExternalResource {

  public abstract URI getConnectionURI();

  public abstract Connection newConnection() throws ConnectionException;

  public abstract IClusterControl getClusterControl();
}
