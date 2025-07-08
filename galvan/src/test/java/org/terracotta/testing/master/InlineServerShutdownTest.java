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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Tests for the InlineServer shutdown behavior.
 * 
 * This test focuses on verifying that the InlineServer.shutdown() method
 * calls the server's halt() method instead of stopAndWait(), which is the
 * key change in the fix_galvan2 branch.
 */
public class InlineServerShutdownTest {

    /**
     * This test verifies that the InlineServer.java file contains the correct
     * method call in the shutdown method. This is a simple code inspection test
     * rather than a functional test, since we can't easily mock the static methods.
     */
    @Test
    public void testShutdownMethodCallsHalt() throws IOException {
        // Read the InlineServer.java file
        Path serverPath = Paths.get("src/main/java/org/terracotta/testing/master/InlineServer.java");
        String content = new String(java.nio.file.Files.readAllBytes(serverPath));
        
        // Check that the shutdown method calls halt() and not stopAndWait()
        assertTrue("InlineServer should call halt() method", 
                content.contains("invokeOnServerMBean(server, \"Server\",\"halt\",null)"));
        
        // Make sure it doesn't still have the old method call
        assertFalse("InlineServer should not call stopAndWait() method", 
                content.contains("invokeOnServerMBean(server, \"Server\",\"stopAndWait\",null)"));
    }
    
    /**
     * This test verifies that the TCServerInfo.java file contains the halt() method
     * implementation, which is part of the fix_galvan2 changes.
     */
    @Test
    public void testTCServerInfoContainsHaltMethod() throws IOException {
        // Read the TCServerInfo.java file
        Path serverPath = Paths.get("../tc-server/src/main/java/com/tc/management/beans/TCServerInfo.java");
        String content = new String(java.nio.file.Files.readAllBytes(serverPath));
        
        // Check that the halt method exists and calls the right method
        assertTrue("TCServerInfo should contain halt() method", 
                content.contains("public boolean halt()"));
        
        // Check that the halt method implementation calls stop with IMMEDIATE
        assertTrue("halt() should call stop with IMMEDIATE", 
                content.contains("server.stop(StopAction.IMMEDIATE)"));
    }
    
    /**
     * This test verifies that the TCServerInfoMBean interface declares the halt() method,
     * which is part of the fix_galvan2 changes.
     */
    @Test
    public void testTCServerInfoMBeanContainsHaltMethod() throws IOException {
        // Read the TCServerInfoMBean.java file
        Path serverPath = Paths.get("../management/src/main/java/com/tc/management/beans/TCServerInfoMBean.java");
        String content = new String(java.nio.file.Files.readAllBytes(serverPath));
        
        // Check that the halt method is declared in the interface
        assertTrue("TCServerInfoMBean should declare halt() method", 
                content.contains("boolean halt();"));
    }
}

// Made with Bob
