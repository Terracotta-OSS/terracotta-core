package com.terracotta.management.l1bridge;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.terracotta.management.service.RemoteAgentBridgeService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Anthony Dahanne
 */
public class RemoteRequestValidatorTest {

  private RemoteRequestValidator requestValidator;

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

  @Test
  public void testValidateAgentSegment() throws Exception {

    List<PathSegment> pathSegements = new ArrayList<PathSegment>();
    pathSegements.add(new PathSegment() {
      @Override
      public String getPath() {
        return "/tc-management-api/agents/cacheManagers";
      }

      @Override
      public MultivaluedMap<String, String> getMatrixParameters() {
        return new MultivaluedMapImpl();
      }
    });
    requestValidator.validateAgentSegment(pathSegements);
    Set<String> validatedNodes = requestValidator.getValidatedNodes();
    assertThat(validatedNodes, (Matcher<? super Set<String>>) hasItems("localhost.home_59822","localhost.home_1212", "localhost.home_4343", "localhost.home_4545"));

  }


  @Test
  public void testValidateAgentSegment__idsOk() throws Exception {

    List<PathSegment> pathSegments = new ArrayList<PathSegment>();
    pathSegments.add(new PathSegment() {
      @Override
      public String getPath() {
        return "/tc-management-api/agents/cacheManagers";
      }

      @Override
      public MultivaluedMap<String, String> getMatrixParameters() {
        MultivaluedMapImpl multivaluedMap = new MultivaluedMapImpl();
        multivaluedMap.add("ids","localhost.home_59822,localhost.home_4545");
        return multivaluedMap;
      }
    });
    requestValidator.validateAgentSegment(pathSegments);
    Set<String> validatedNodes = requestValidator.getValidatedNodes();
    assertThat(validatedNodes, (Matcher<? super Set<String>>) hasItems("localhost.home_59822", "localhost.home_4545"));

  }

  @Test
  public void testValidateAgentSegment__idsNotOk() throws Exception {

    List<PathSegment> pathSegments = new ArrayList<PathSegment>();
    pathSegments.add(new PathSegment() {
      @Override
      public String getPath() {
        return "/tc-management-api/agents/cacheManagers";
      }

      @Override
      public MultivaluedMap<String, String> getMatrixParameters() {
        MultivaluedMapImpl multivaluedMap = new MultivaluedMapImpl();
        multivaluedMap.add("ids","blouf,localhost.home_4545");
        return multivaluedMap;
      }
    });
    ResourceRuntimeException e = null;
    try {
      requestValidator.validateAgentSegment(pathSegments);
    } catch (ResourceRuntimeException rre) {
      e = rre;
    }
    assertEquals("Agent IDs must be in " +
            "'[localhost.home_1212, localhost.home_4343, localhost.home_4545, localhost.home_59822]' " +
            "or 'embedded'.", e.getMessage());

  }

}
