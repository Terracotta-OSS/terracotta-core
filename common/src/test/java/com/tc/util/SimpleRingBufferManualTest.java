/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manual test class for SimpleRingBuffer that doesn't rely on JUnit
 */
public class SimpleRingBufferManualTest {
    
    public static void main(String[] args) {
        testEmptyBuffer();
        testPartiallyFilledBuffer();
        testFullBufferNoWraparound();
        testWraparoundBuffer();
        testMultipleWraparounds();
        testNullRejection();
        testSingleElement();
        
        System.out.println("All tests completed!");
    }
    
    private static void testEmptyBuffer() {
        System.out.println("Testing empty buffer...");
        SimpleRingBuffer<String> buffer = new SimpleRingBuffer<>(5);
        Iterator<String> iterator = buffer.iterator();
        
        // Empty buffer should have no elements
        if (iterator.hasNext()) {
            System.err.println("FAIL: Empty buffer should not have elements");
        } else {
            System.out.println("PASS: Empty buffer has no elements");
        }
    }
    
    private static void testPartiallyFilledBuffer() {
        System.out.println("Testing partially filled buffer...");
        SimpleRingBuffer<String> buffer = new SimpleRingBuffer<>(5);
        buffer.put("A");
        buffer.put("B");
        buffer.put("C");
        
        // Buffer has 3 elements, not wrapped around
        List<String> elements = collectElements(buffer);
        System.out.println("Elements: " + elements);
        
        if (elements.size() != 3) {
            System.err.println("FAIL: Expected 3 elements, got " + elements.size());
        } else if (!elements.get(0).equals("A") || !elements.get(1).equals("B") || !elements.get(2).equals("C")) {
            System.err.println("FAIL: Elements in wrong order: " + elements);
        } else {
            System.out.println("PASS: Partially filled buffer has correct elements in order");
        }
    }
    
    private static void testFullBufferNoWraparound() {
        System.out.println("Testing full buffer without wraparound...");
        SimpleRingBuffer<String> buffer = new SimpleRingBuffer<>(3);
        buffer.put("A");
        buffer.put("B");
        buffer.put("C");
        
        // Buffer is full but hasn't wrapped around
        List<String> elements = collectElements(buffer);
        System.out.println("Elements: " + elements);
        
        if (elements.size() != 3) {
            System.err.println("FAIL: Expected 3 elements, got " + elements.size());
        } else if (!elements.get(0).equals("A") || !elements.get(1).equals("B") || !elements.get(2).equals("C")) {
            System.err.println("FAIL: Elements in wrong order: " + elements);
        } else {
            System.out.println("PASS: Full buffer has correct elements in order");
        }
    }
    
    private static void testWraparoundBuffer() {
        System.out.println("Testing buffer with wraparound...");
        SimpleRingBuffer<String> buffer = new SimpleRingBuffer<>(3);
        buffer.put("A");
        buffer.put("B");
        buffer.put("C");
        buffer.put("D"); // Overwrites A
        
        // Buffer has wrapped around, D replaced A
        List<String> elements = collectElements(buffer);
        System.out.println("Elements: " + elements);
        
        if (elements.size() != 3) {
            System.err.println("FAIL: Expected 3 elements, got " + elements.size());
        } else if (!elements.get(0).equals("B") || !elements.get(1).equals("C") || !elements.get(2).equals("D")) {
            System.err.println("FAIL: Elements in wrong order: " + elements);
        } else {
            System.out.println("PASS: Wrapped buffer has correct elements in order");
        }
    }
    
    private static void testMultipleWraparounds() {
        System.out.println("Testing buffer with multiple wraparounds...");
        SimpleRingBuffer<String> buffer = new SimpleRingBuffer<>(3);
        buffer.put("A");
        buffer.put("B");
        buffer.put("C");
        buffer.put("D"); // Overwrites A
        buffer.put("E"); // Overwrites B
        buffer.put("F"); // Overwrites C
        
        // After multiple wraparounds
        List<String> elements = collectElements(buffer);
        System.out.println("Elements: " + elements);
        
        if (elements.size() != 3) {
            System.err.println("FAIL: Expected 3 elements, got " + elements.size());
        } else if (!elements.get(0).equals("D") || !elements.get(1).equals("E") || !elements.get(2).equals("F")) {
            System.err.println("FAIL: Elements in wrong order: " + elements);
        } else {
            System.out.println("PASS: Multiple wraparound buffer has correct elements in order");
        }
    }
    
    private static void testNullRejection() {
        System.out.println("Testing null rejection...");
        SimpleRingBuffer<String> buffer = new SimpleRingBuffer<>(3);
        try {
            buffer.put(null);
            System.err.println("FAIL: Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            System.out.println("PASS: Null was rejected with NullPointerException");
        }
    }
    
    private static void testSingleElement() {
        System.out.println("Testing single element buffer...");
        SimpleRingBuffer<String> buffer = new SimpleRingBuffer<>(1);
        buffer.put("A");
        buffer.put("B"); // Overwrites A
        
        List<String> elements = collectElements(buffer);
        System.out.println("Elements: " + elements);
        
        if (elements.size() != 1) {
            System.err.println("FAIL: Expected 1 element, got " + elements.size());
        } else if (!elements.get(0).equals("B")) {
            System.err.println("FAIL: Expected element B, got " + elements.get(0));
        } else {
            System.out.println("PASS: Single element buffer contains only the newest element");
        }
    }
    
    // Helper method to collect all elements from the buffer into a list
    private static <T> List<T> collectElements(SimpleRingBuffer<T> buffer) {
        List<T> result = new ArrayList<>();
        for (T element : buffer) {
            result.add(element);
        }
        return result;
    }
}

// Made with Bob
