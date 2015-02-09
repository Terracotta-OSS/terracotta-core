package com.terracotta.management.l1bridge;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;

import com.terracotta.management.service.L1MBeansSource;
import com.terracotta.management.service.RemoteAgentBridgeService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;

/**
 * @author Anthony Dahanne
 */
public class RemoteRequestValidatorTest {

  private RemoteRequestValidator requestValidator;

  @Before
  public void setUp() throws Exception {
    RemoteAgentBridgeService remoteAgentBridgeService = mock(RemoteAgentBridgeService.class);
    L1MBeansSource l1MBeansSource = mock(L1MBeansSource.class);
    HashSet<String> agentNodeNames = new HashSet<String>();
    agentNodeNames.add("localhost.home_59822");
    agentNodeNames.add("localhost.home_1212");
    agentNodeNames.add("localhost.home_4343");
    agentNodeNames.add("localhost.home_4545");

    when(remoteAgentBridgeService.getRemoteAgentNodeNames()).thenReturn(agentNodeNames);
    when(l1MBeansSource.containsJmxMBeans()).thenReturn(true);
    requestValidator = new RemoteRequestValidator(remoteAgentBridgeService, l1MBeansSource);
    requestValidator.setValidatedNodes(new HashSet<String>());
  }

  @Test
  public void testValidateAgentSegment() throws Exception {
    List<PathSegment> pathSegments = new ArrayList<PathSegment>();
    pathSegments.add(new PathSegmentImpl("tc-management-api"));
    pathSegments.add(new PathSegmentImpl("agents"));
    pathSegments.add(new PathSegmentImpl("cacheManagers"));

    requestValidator.validateAgentSegment(pathSegments);
    Set<String> validatedNodes = requestValidator.getValidatedNodes();
    assertThat(validatedNodes, hasItems("localhost.home_59822","localhost.home_1212", "localhost.home_4343", "localhost.home_4545"));
  }


  @Test
  public void testValidateAgentSegment__idsOk() throws Exception {
    List<PathSegment> pathSegments = new ArrayList<PathSegment>();
    pathSegments.add(new PathSegmentImpl("tc-management-api"));
    pathSegments.add(new PathSegmentImpl("agents", new MultivaluedHashMap<String, String>(){{
      add("ids","localhost.home_59822,localhost.home_4545");
    }}));
    pathSegments.add(new PathSegmentImpl("cacheManagers"));
    requestValidator.validateAgentSegment(pathSegments);
    Set<String> validatedNodes = requestValidator.getValidatedNodes();
    assertThat(validatedNodes, hasItems("localhost.home_59822", "localhost.home_4545"));
  }

  @Test
  public void testValidateAgentSegment__idsNotOk() throws Exception {
    List<PathSegment> pathSegments = new ArrayList<PathSegment>();
    pathSegments.add(new PathSegmentImpl("tc-management-api"));
    pathSegments.add(new PathSegmentImpl("agents", new MultivaluedHashMap<String, String>(){{
      add("ids","blouf,localhost.home_4545");
    }}));
    pathSegments.add(new PathSegmentImpl("cacheManagers"));

    ResourceRuntimeException e = null;
    try {
      requestValidator.validateAgentSegment(pathSegments);
    } catch (ResourceRuntimeException rre) {
      e = rre;
    }
    assertThat(e, is(notNullValue()));
    assertThat(e.getMessage(), startsWith("Agent IDs must be in"));
    assertThat(e.getMessage(), containsString("localhost.home_1212"));
    assertThat(e.getMessage(), containsString("localhost.home_4343"));
    assertThat(e.getMessage(), containsString("localhost.home_4545"));
    assertThat(e.getMessage(), containsString("localhost.home_59822"));
    assertThat(e.getMessage(), endsWith("or 'embedded'."));
  }

  @Test
  public void getAgentIdsFromPathSegments_v1Test() {
    String idsExpected = "localhost_666,localhost_777";
    RemoteRequestValidator remoteRequestValidator = new RemoteRequestValidator(null, null);

    List<PathSegment> pathSegments = new ArrayList<PathSegment>();
    PathSegment pathSegmentAgents = mock(PathSegment.class);
    when(pathSegmentAgents.getPath()).thenReturn("agents");
    MultivaluedMap<String, String> idsMatrixParameters = new MultivaluedHashMap<String, String>();
    idsMatrixParameters.add("ids", idsExpected);
    when(pathSegmentAgents.getMatrixParameters()).thenReturn(idsMatrixParameters);

    PathSegment pathSegmentCacheManagers = mock(PathSegment.class);
    when(pathSegmentCacheManagers.getPath()).thenReturn("cacheManagers");

    pathSegments.add(pathSegmentAgents);
    pathSegments.add(pathSegmentCacheManagers);

    assertEquals(idsExpected, remoteRequestValidator.getAgentIdsFromPathSegments(pathSegments));

  }

  @Test
  public void getAgentIdsFromPathSegments_v2Test() {
    String idsExpected = "localhost_666,localhost_777";
    RemoteRequestValidator remoteRequestValidator = new RemoteRequestValidator(null, null);

    List<PathSegment> pathSegments = new ArrayList<PathSegment>();
    PathSegment pathSegmentVersion = mock(PathSegment.class);
    when(pathSegmentVersion.getPath()).thenReturn("v2");
    PathSegment pathSegmentAgents = mock(PathSegment.class);
    when(pathSegmentAgents.getPath()).thenReturn("agents");
    MultivaluedMap<String, String> idsMatrixParameters = new MultivaluedHashMap<String, String>();
    idsMatrixParameters.add("ids", idsExpected);
    when(pathSegmentAgents.getMatrixParameters()).thenReturn(idsMatrixParameters);
    PathSegment pathSegmentCacheManagers = mock(PathSegment.class);
    when(pathSegmentCacheManagers.getPath()).thenReturn("cacheManagers");

    pathSegments.add(pathSegmentVersion);
    pathSegments.add(pathSegmentAgents);
    pathSegments.add(pathSegmentCacheManagers);

    assertEquals(idsExpected, remoteRequestValidator.getAgentIdsFromPathSegments(pathSegments));

  }

  static class PathSegmentImpl implements PathSegment {
    private String path;
    private MultivaluedMap<String, String> matrixParameters = new MultivaluedHashMap<String, String>();

    PathSegmentImpl(String path) {
      this.path = path;
    }

    PathSegmentImpl(String path, MultivaluedMap<String, String> matrixParameters) {
      this.path = path;
      this.matrixParameters = matrixParameters;
    }

    @Override
    public String getPath() {
      return path;
    }

    @Override
    public MultivaluedMap<String, String> getMatrixParameters() {
      return matrixParameters;
    }
  }

}
