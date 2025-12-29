/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.master;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class ServerDeploymentBuilderTest {

  public ServerDeploymentBuilderTest() {
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
   */
  @Test
  public void testInstallPath() throws Exception {
    Path p = Paths.get(System.getProperty("kitInstallPath")).resolve("galvan-test-server");
    p = new ServerDeploymentBuilder().installPath(p).deploy();
    Stream<Path> s = Files.walk(p);
    Assert.assertTrue(s.anyMatch(w->w.getFileName().toString().equals("tc.jar")));
  }
  /**
   */
  @Test
  public void testTempPath() throws Exception {
    Path p = new ServerDeploymentBuilder().deploy();
    System.out.println("the server path " + p);
    Stream<Path> s = Files.walk(p);
    Assert.assertTrue(s.peek(System.out::println).anyMatch(w->w.getFileName().toString().equals("tc.jar")));
  }

  @Test
  public void testRefresh() throws Exception {
    Path p = Paths.get(System.getProperty("kitInstallPath")).resolve("galvan-test-server");
    p = new ServerDeploymentBuilder().installPath(p).deploy();
    Assert.assertTrue(p.resolve("testprobe").toFile().createNewFile());
    p = new ServerDeploymentBuilder().installPath(p).deploy(true);
    Assert.assertTrue(Files.find(p, 1, (path,a)->path.getFileName().toString().equals("testprobe")).findAny().isEmpty());
  }
}
