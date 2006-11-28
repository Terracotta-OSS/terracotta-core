/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.util;

public class TestUtil
{
    public static void printStats(String name, String value)
    {
        System.out.println("**%% TERRACOTTA TEST STATISTICS %%**: value=" + name + " units=" + value);
    }
}