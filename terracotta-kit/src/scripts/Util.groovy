
class Util {
    static final def ant = new AntBuilder()
    
    static boolean rename(String oldFile, String newFile) {
      return new File(oldFile).renameTo(new File(newFile))  
    }
    
    static void processEhcacheDistribution(project) {
      def ehcacheVersion = project.properties['ehcache.version']
      def kitEdition = project.properties['kit.edition']
      def rootDir = project.properties['rootDir']
      
      def rootEhcacheFolder = "/ehcache-core-" + ehcacheVersion
      if (kitEdition == 'enterprise') {
        rootEhcacheFolder = "/ehcache-core-ee-" + ehcacheVersion
      }
  
      def currentName =  rootDir + rootEhcacheFolder
      def newName = rootDir  + "/ehcache"
      rename(currentName, newName) 
    }                  
    
    static void processQuartzDistribution(project) {
      def quartzVersion = project.properties['quartz.version']
      def rootDir = project.properties['rootDir']
      def kitEdition = project.properties['kit.edition']
      
      def currentName =  rootDir + "/quartz-" + quartzVersion
      def newName = rootDir  + "/quartz"
      rename(currentName, newName)
      if (kitEdition == "enterprise") {        
        ant.replace(dir: newName, includes: "**/example15/instance*.properties", 
                    token: "TerracottaJobStore", value: "EnterpriseTerracottaJobStore")
      }
    }                  
    
    static void createFeatureJar(project) {
      def rootDir = project.properties['rootDir']
      def featureJar = "org.terracotta.dso_" + project.properties['eclipse.plugin.version']
      def destFile = rootDir + "/update/features/" + featureJar + ".jar" 
      def baseDir =  rootDir + "/update/features"
      ant.jar(destfile: destFile, basedir: baseDir)
      ant.delete(file: rootDir + "/update/features/feature.xml")
    }
    
    static void createPluginJar(project) {
      def rootDir = project.properties['rootDir']
      def pluginJar = "org.terracotta.dso_" + project.properties['eclipse.plugin.version']
      def destFile = rootDir + "/update/plugins/" + pluginJar + ".jar" 
      def baseDir =  rootDir + "/update/plugins"
      def manifestFile = baseDir + "/META-INF/MANIFEST.MF"
      // generate classpath from lib folder
      def bundleClasspath = new StringBuilder()
      def libs = new File(baseDir, "lib").listFiles()
      for (def file : libs) {
        bundleClasspath.append("lib/").append(file.getName()).append(",")
      }
      if (bundleClasspath.length() > 0) {
        bundleClasspath.deleteCharAt(bundleClasspath.length() - 1)
      }
      ant.replace(file: manifestFile, token: "@BUNDLE-CLASSPATH@", value: bundleClasspath.toString())
      ant.jar(destfile: destFile, basedir: baseDir, manifest: manifestFile)
      ant.delete(includeEmptyDirs: "true") {
        ant.fileset(dir: baseDir, excludes: pluginJar + ".jar")
      }
    }
    
    static void processTerracottaJar(project, tcJar, targetDir) {
      def rootDir = project.properties['rootDir']
      def destFile = rootDir + "/" + targetDir + "/tc.jar"

      ant.zip(destfile: destFile) {
        zipfileset(src: tcJar.toString(), excludes: "**/build-data.txt,**/tim-get.properties")
      }
      
      ant.delete(file: tcJar.toString())
    }
    
    static void setPermission(project) {
      def rootDir = project.properties['rootDir']
      ant.chmod(dir: rootDir, perm: "a+x",
                includes: "**/*.sh,**/*.bat,**/*.exe,**/bin/**,**/lib/**")
    }
    
    static void setEclipsePluginVersion(project, revision, timestamp) {
      // Eclipse plugin versioning: major.minor.service.qualifier
      def m = project.version =~ /(\d+)\.(\d+)\.(\d+)(.*)/
      if (m.matches()) {
        def major = m[0][1]
        def minor = m[0][2]
        def service = m[0][3]
        def tag = m[0][4] != null ? m[0][4] : ""
        tag = tag.replace("-", ".")
        def qualifier = "r" + revision + "_v" + timestamp + tag
        def pluginVersion = major + "." + minor + "." + service + "." + qualifier
        project.properties.setProperty('eclipse.plugin.version', pluginVersion)
      } else {
        throw new RuntimeException("version doesn't match Eclipse plugin pattern")
      }
    }  

    static void processEnterpriseGtKit(project) {
      def rootDir = project.properties['rootDir']
      def binDir = new File(rootDir, "bin")
      def libDir = new File(rootDir, "lib")
      ant.delete(verbose: "true") {
        fileset(dir: libDir, includes: "je-*.jar")
      }
      ant.replace(file: new File(binDir, "start-tc-server.sh"),
                  token: "USE_DERBYDB=false",
                  value: "USE_DERBYDB=true",
                  failOnNoReplacements: "true")
      ant.replace(file: new File(binDir, "start-tc-server.bat"),
                  token: "set USE_DERBYDB=\"false\"",
                  value: "set USE_DERBYDB=\"true\"",
                  failOnNoReplacements: "true")
                  
    }    
}
