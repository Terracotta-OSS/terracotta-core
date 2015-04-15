/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.context.ServerMapEvictionContext;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import java.util.Map;

/**
 *
 * @author mscott
 */
public class BrakingEvictionTrigger extends AbstractEvictionTrigger {
    
    private final int turns ;

    public BrakingEvictionTrigger(ObjectID oid, int turns) {
        super(oid);
        this.turns = turns;
    }

    @Override
    public ServerMapEvictionContext collectEvictionCandidates(int targetMax, String className, EvictableMap map, ClientObjectReferenceSet clients) {
        int size = map.getSize();
        
        Map sampled = map.getRandomSamples(Math.round(size*turns/10000f),clients, SamplingType.FOR_EVICTION);

        return createEvictionContext(className, sampled);
    }

    
}
