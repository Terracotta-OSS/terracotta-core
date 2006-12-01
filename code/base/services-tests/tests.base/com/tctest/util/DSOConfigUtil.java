/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.util;

import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;

public class DSOConfigUtil
{
    public static void addWriteAutolock(DSOClientConfigHelper config, Class clazz)
    {
        config.addWriteAutolock("* " + clazz.getName() + "*.*(..)");
    }
    
    public static void addRoot(TransparencyClassSpec spec, String root)
    {
        spec.addRoot(root, root); 
    }
    
    public static void autoLockAndInstrumentClass(DSOClientConfigHelper config, Class clazz)
    {
        autoLockAndInstrumentClass(config, clazz, false);
    }

    public static void autoLockAndInstrumentClass(DSOClientConfigHelper config, Class clazz, boolean honorTransient)
    {
        config.addIncludePattern(clazz.getName(), honorTransient);
        config.addIncludePattern(clazz.getName() + "$*");
   
        addWriteAutolock(config, clazz);
    }
}
