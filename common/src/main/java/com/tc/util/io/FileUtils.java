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
