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
package com.tc.services;

import com.tc.management.beans.TCDumper;
import com.tc.server.TCServerMain;
import java.io.InputStream;
import org.terracotta.monitoring.PlatformService;

/**
 * @author vmad
 */
public class PlatformServiceImpl implements PlatformService {

    private final TCDumper tcDumper;

    public PlatformServiceImpl(TCDumper tcDumper) {
        this.tcDumper = tcDumper;
    }

    @Override
    public void dumpPlatformState() {
        this.tcDumper.dump();
    }
    
    @Override
    public InputStream getPlatformConfiguration() {
      return TCServerMain.getSetupManager().rawConfigFile();
    }
}
