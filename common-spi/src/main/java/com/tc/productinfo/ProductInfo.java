/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.productinfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Utility class to retrieve the build information for the product.
 */
public final class ProductInfo {
  
  private static final String               DATE_FORMAT                = "yyyyMMdd-HHmmss";
  private static final Pattern              KITIDPATTERN               = Pattern.compile("(\\d+\\.\\d+.\\d+).*");

  public static final String                UNKNOWN_VALUE              = "[unknown]";

  private static ProductInfo                INSTANCE                   = null;

  private final BuildInfo                      buildInfo;
  private final List<PatchInfo>                patchInfo = new ArrayList<>();
  private final List<ExtensionInfo>            extensionInfo = new ArrayList<>();
  private final String                         kitID;
  private String                         buildID;
  /**
   * Construct a ProductInfo by reading properties from streams (most commonly by loading properties files as resources
   * from the classpath). If an IOException occurs while loading the build or patch streams, the System will exit. These
   * resources are always expected to be in the path and are necessary to do version compatibility checks.
   * 
   * @param buildData Build properties in stream conforming to Java Properties file format, must not be null
   * @param patchData Patch properties in stream conforming to Java Properties file format, null if none
   * @throws NullPointerException If buildData is null and assertions are enabled
   * @throws IOException If there is an error reading the build or patch data streams
   * @throws ParseException If there is an error reading the timestamp format in build or patch data streams
   */
  ProductInfo(BuildInfo build, List<PatchInfo> patches, List<ExtensionInfo> extensions) throws IOException {    
    if (build == null) {
      build = new BaseBuildInfo(getClass().getResourceAsStream("/build-data.txt"));
    }
    buildInfo = build;

    if (patches != null) {
      patchInfo.addAll(patches);
    }

    // Get all patch build properties
    Collections.sort(patchInfo, (a, b)->b.count() - a.count());

    if (extensions != null) {
      this.extensionInfo.addAll(extensions);
    }

    Matcher matcher = KITIDPATTERN.matcher(buildInfo.getVersion());
    kitID = matcher.matches() ? matcher.group(1) : UNKNOWN_VALUE;
  }

  static Date parseTimestamp(String timestampString) throws ParseException {
    return (timestampString == null) ? null : new SimpleDateFormat(DATE_FORMAT).parse(timestampString);
  }

  public static ProductInfo getInstance() {
    return getInstance(ProductInfo.class.getClassLoader());
  }

  public static synchronized ProductInfo getInstance(ClassLoader loader) {
    if (INSTANCE == null) {
      try {
        ServiceLoader<Description> additional = ServiceLoader.load(Description.class, loader);
        Iterator<Description> steps = additional.iterator();
        BuildInfo build = null;
        List<PatchInfo> patches = new LinkedList<>();
        List<ExtensionInfo> info = new LinkedList<>();
        while (steps.hasNext()) {
          Description next = steps.next();
          if (next instanceof BuildInfo) {
            if (build != null) {
              throw new RuntimeException("corrupt installation. multiple builds detected");
            }
            build = (BuildInfo)next;
          }
          if (next instanceof PatchInfo) {
            patches.add((PatchInfo)next);
          }
          if (next instanceof ExtensionInfo) {
            info.add((ExtensionInfo)next);
          }
        }
        INSTANCE = new ProductInfo(build, patches, info);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return INSTANCE;
  }

  static InputStream getData(String name) {
    CodeSource codeSource = ProductInfo.class.getProtectionDomain().getCodeSource();
    if (codeSource != null && codeSource.getLocation() != null) {
      URL source = codeSource.getLocation();

      if (source.getProtocol().equals("file") && source.toExternalForm().endsWith(".jar")) {
        URL res;
        try {
          res = new URL("jar:" + source.toExternalForm() + "!" + name);
          InputStream in = res.openStream();
          if (in != null) { return in; }
        } catch (MalformedURLException e) {
          throw new AssertionError(e);
        } catch (IOException e) {
          // must not be embedded in this jar -- resolve via loader path
        }
      } else if (source.getProtocol().equals("file") && (new File(source.getPath()).isDirectory())) {
        File local = new File(source.getPath(), name);

        if (local.isFile()) {
          try {
            return new FileInputStream(local);
          } catch (FileNotFoundException e) {
            throw new AssertionError(e);
          }
        }
      }
    }

    return ProductInfo.class.getResourceAsStream(name);
  }
  
  public Collection<String> getExtensions() {
    return this.extensionInfo.stream().map(e->e.getExtensionInfo()).collect(Collectors.toList());
  }

  public String moniker() {
    return buildInfo.getMonkier();
  }

  public String version() {
    return buildInfo.getVersion();
  }

  public String versionMessage() {
    return buildInfo.getVersionMessage();
  }

  public String mavenArtifactsVersion() {
    return buildInfo.getVersion();
  }

  public String buildVersion() {
    return buildInfo.getVersion();
  }

  public String kitID() {
    return kitID;
  }

  public String buildTimestamp() {
    return buildInfo.getTimestamp();
  }

  public String buildTimestampAsString() {
    return buildInfo.getTimestamp();
  }

  public String buildBranch() {
    return buildInfo.getBranch();
  }

  public String copyright() {
    return buildInfo.getCopyright();
  }

  public String buildRevision() {
    return buildInfo.getRevision();
  }

  public boolean isPatched() {
    return !patchInfo.isEmpty();
  }

  public String patchLevel() {
    return patchInfo.get(0).getLevel();
  }

  public String patchTimestamp() {
    return patchInfo.get(0).getTimestamp();
  }

  public String patchRevision() {
    return patchInfo.get(0).getRevision();
  }

  public String patchBranch() {
    return patchInfo.get(0).getBranch();
  }

  public String toShortString() {
    return buildInfo.getMonkier() + " " + buildInfo.getVersion();
  }

  public String toLongString() {
    return toShortString() + ", as of " + buildID();
  }

  public String buildID() {
    if (buildID == null) {
      buildID = buildInfo.getTimestamp() + " (Revision " + buildInfo.getRevision() + " from " + buildInfo.getBranch() + ")";
    }
    return buildID;
  }

  public String toLongPatchString() {
    return toShortPatchString() + ", as of " + patchBuildID();
  }

  public String toShortPatchString() {
    return "Patch Level " + patchInfo.get(0).getLevel();
  }

  public String patchBuildID() {
    return patchTimestamp() + " (Revision " + patchInfo.get(0).getRevision() + " from "
           + patchInfo.get(0).getBranch()+ ")";
  }

  @Override
  public String toString() {
    return toShortString();
  }
}
