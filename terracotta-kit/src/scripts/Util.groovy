
class Util {
    static final def ant = new AntBuilder()
    
    static void rename(String oldFile, String newFile) {
      boolean success = new File(oldFile).renameTo(new File(newFile))
      if (!success) {
        throw new RuntimeException("Failed to rename '" + oldFile + "' to '" + newFile + "'")
      } 
    }
    
    static void processEhcacheDistribution(project) {
      def ehcacheVersion = project.properties['ehcache.version']
      def kitEdition = project.properties['kit.edition']
      def rootDir = project.properties['rootDir']
      
      def prefix =  kitEdition == 'enterprise' ? "/ehcache-ee-" : "/ehcache-"
      def rootEhcacheFolder = prefix + ehcacheVersion
  
      def currentName =  rootDir + rootEhcacheFolder
      def newName = rootDir  + "/ehcache"
      rename(currentName, newName) 
    }                  
    
    static void processQuartzDistribution(project) {
      def quartzVersion = project.properties['quartz.version']
      def kitEdition = project.properties['kit.edition']
      def rootDir = project.properties['rootDir']
      
      def prefix =  kitEdition == 'enterprise' ? "/quartz-ee-" : "/quartz-"
      def rootQuartzFolder = prefix + quartzVersion
      
      def currentName =  rootDir + rootQuartzFolder
      def newName = rootDir  + "/quartz"
      rename(currentName, newName)
      if (kitEdition == "enterprise") {        
        ant.replace(dir: newName, includes: "**/example15/instance*.properties", 
                    token: "TerracottaJobStore", value: "EnterpriseTerracottaJobStore")
      }
    }                  
    
    static void processTerracottaJar(project, tcJar, targetDir) {
      def rootDir = project.properties['rootDir']
      def destFile = rootDir + "/" + targetDir + "/tc.jar"

      ant.zip(destfile: destFile) {
        zipfileset(src: tcJar.toString(), excludes: "**/build-data.txt")
      }
      
      ant.delete(file: tcJar.toString())
    }
    
    static void setPermission(project) {
      def rootDir = project.properties['rootDir']
      ant.chmod(dir: rootDir, perm: "a+x",
                includes: "**/*.sh,**/*.bat,**/*.exe,**/bin/**,**/lib/**")
    }
    
    static final String BUILD_DATA_FILE_NAME = "build-data.txt"
    static final String PATCH_DATA_FILE_NAME = "patch-data.txt"
    static final String RESOURCES_DIR = "lib/resources"

    static String createPatchDataFile(project, patchLevel) {
      def rootDir = project.properties['rootDir']
      def resourcesDir = new File(rootDir, RESOURCES_DIR)

      File buildDataFile = new File(resourcesDir, BUILD_DATA_FILE_NAME)
      Properties buildData = new Properties()
      buildDataFile.withReader { reader ->
        buildData.load(reader)
      }

      Properties patchData = new Properties()
      patchData['terracotta.patch.level'] = patchLevel
      buildData.each { key, value ->
        def patchDataKey = key.replaceAll(/\.build\./, ".patch.")
        patchData[patchDataKey] = value
      }
      File patchDataFile = new File(resourcesDir, PATCH_DATA_FILE_NAME)
      patchDataFile.withWriter { writer ->
        patchData.store(writer, null)
      }
      return RESOURCES_DIR + File.separator + PATCH_DATA_FILE_NAME
    }
}
