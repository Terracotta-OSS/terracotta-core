/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.entity.VoltronEntityMessage;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.terracotta.config.Entities;
import org.terracotta.config.Entity;
import org.terracotta.config.TcConfig;

/**
 *
 * @author mscott
 */
public class PermanentEntityParserTest {
  
  public PermanentEntityParserTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

  /**
   * Test of parseEntities method, of class PermanentEntityParser.
   */
  @Test
  public void testParseEntitiesNullChecks() {
    TcConfig config = mock(TcConfig.class);
    List<Entity> perms = new ArrayList<>();
    Entity e1 = mock(Entity.class);
    when(e1.getConfiguration()).thenReturn(mock(Entity.Configuration.class));
    perms.add(e1);
    Entity e2 = mock(Entity.class);
    perms.add(e2);
    Entity e3 = mock(Entity.class);
    Entity.Configuration ec = mock(Entity.Configuration.class);
    when(ec.getProperties()).thenReturn(mock(Entity.Configuration.Properties.class));
    when(e1.getConfiguration()).thenReturn(ec);
    perms.add(e3);
    
    Entities list = mock(Entities.class);
    when(list.getEntity()).thenReturn(perms);
    when(config.getEntities()).thenReturn(list);
    List<VoltronEntityMessage> msgs = PermanentEntityParser.parseEntities(config);
    for (VoltronEntityMessage msg : msgs) {
      Assert.assertNotNull(msg.getExtendedData());
    }
  }
  
}
