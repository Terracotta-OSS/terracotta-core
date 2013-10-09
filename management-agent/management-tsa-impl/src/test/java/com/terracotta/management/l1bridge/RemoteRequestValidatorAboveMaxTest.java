package com.terracotta.management.l1bridge;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.terracotta.management.service.RemoteAgentBridgeService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Anthony Dahanne
 *         This class is the same as RemoteRequestValidatorTest ;  but I had to separate the methods because of the use a
 *         VM argument that sets a static field
 */
public class RemoteRequestValidatorAboveMaxTest {

  private RemoteRequestValidator requestValidator;

  @BeforeClass
  public static void staticSetUp() {
    System.setProperty("com.terracotta.agent.defaultMaxClientsToDisplay", "3");
  }

  @Before
  public void setUp() throws Exception {
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    HashSet<String> agentNodeNames = new HashSet<String>();
    agentNodeNames.add("localhost.home_59822");
    agentNodeNames.add("localhost.home_1212");
    agentNodeNames.add("localhost.home_4343");
    agentNodeNames.add("localhost.home_4545");

    when(remoteAgentBridgeService.getRemoteAgentNodeNames()).thenReturn(agentNodeNames);
    requestValidator = new RemoteRequestValidator(remoteAgentBridgeService);
    requestValidator.setValidatedNodes(new HashSet<String>());
  }


  /**
   * If the don't specify the client ids and their number is greater than the max, we return an error
   *
   * @throws Exception
   */
  @Test
  public void testValidateAgentSegment__limit3() throws Exception {

    List<PathSegment> pathSegments = new ArrayList<PathSegment>();
    pathSegments.add(new PathSegment() {
      @Override
      public String getPath() {
        return "/tc-management-api/agents/cacheManagers";
      }

      @Override
      public MultivaluedMap<String, String> getMatrixParameters() {
        return new MultivaluedMapImpl();
      }
    });
    ResourceRuntimeException e = null;
    try {
      requestValidator.validateAgentSegment(pathSegments);
    } catch (ResourceRuntimeException rre) {
      e = rre;
    }
    assertEquals("There are more than 3 agents available; you have to change the maximum using the VM argument " +
            "com.terracotta.agent.defaultMaxClientsToDisplay or you have to specify each agent ID. " +
            "Agent IDs must be in " +
            "'[localhost.home_1212, localhost.home_4343, localhost.home_4545, localhost.home_59822]' " +
            "or 'embedded'.", e.getMessage());

  }

}
