/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.aspectwerkz;

import com.tc.aspectwerkz.reflect.ClassInfo;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ClassInfoFactory {
    private final Map classInfoCache = new HashMap();

    public ClassInfo getClassInfo(String className) {
        ClassInfo info;
        synchronized (classInfoCache) {
            info = (ClassInfo) classInfoCache.get(className);
            if (info == null) {
                info = new SimpleClassInfo(className);
                classInfoCache.put(className, info);
            }
        }
        return info;
    }
}
