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
 *
 */
package com.tc.async.impl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Stage;
import com.tc.util.State;
import com.tc.util.concurrent.SetOnceFlag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;



/**
 *
 */
public class StageControllerTest {
  
  public StageControllerTest() {
  }
  
  @BeforeAll
  public static void setUpClass() {
  }
  
  @AfterAll
  public static void tearDownClass() {
  }
  
  @BeforeEach
  public void setUp() {
  }
  
  @AfterEach
  public void tearDown() {
  }
  /**
   * Test of addTriggerToState method, of class StageController.
   */
  @Test
  public void testAddTriggerToState() {
    System.out.println("addTriggerToState");
    
    State init = new State("INIT");
    State test = new State("TeST");
    
    StageController instance = new StageController();
    final SetOnceFlag didRun = new SetOnceFlag();
    ConfigurationContext cxt = mock(ConfigurationContext.class);

    final Stage prestage = mock(Stage.class);
    when(cxt.getStage(Matchers.eq("PRE"), Matchers.any(Class.class))).then(new Answer<Stage>() {
      @Override
      public Stage answer(InvocationOnMock invocation) throws Throwable {
        assertFalse(didRun.isSet(), () -> "PRE");
        return prestage;
      }
    });

    final Stage poststage = mock(Stage.class);
    when(cxt.getStage(Matchers.eq("POST"), Matchers.any(Class.class))).then(new Answer<Stage>() {
      @Override
      public Stage answer(InvocationOnMock invocation) throws Throwable {
        assertTrue(didRun.isSet(), () -> "POST");
        return poststage;
      }
    });
    
    final Stage i = mock(Stage.class);
    when(cxt.getStage(Matchers.eq("INIT"), Matchers.any(Class.class))).then(new Answer<Stage>() {
      @Override
      public Stage answer(InvocationOnMock invocation) throws Throwable {
        assertFalse(didRun.isSet(), () -> "INIT");
        return i;
      }
    });
    
    instance.addStageToState(init, "INIT");
    instance.addStageToState(test, "PRE");
    instance.addTriggerToState(test, new Runnable() {
      public void run() {didRun.set();}
    });
    instance.addStageToState(test, "POST");

    instance.transition(cxt, init, test);
    assertTrue(didRun.isSet());
    verify(prestage).start(cxt);
    verify(poststage).start(cxt);
    verify(i).destroy();
  }
  
}
