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
package com.tc.util.io;

import java.io.File;
import java.io.IOException;

/**
 * @author vmad
 */
public class FileUtils {
    public static void forceMkdir(File directory) throws IOException {
        if(directory.exists()) {
            if(!directory.isDirectory()) {
                throw new IOException("A file with given directory name (" + directory + ") exists");
            }
        } else {
            if(!directory.mkdirs()) {
                throw new IOException("Couldn't create directory " + directory);
            }
        }
    }
}
