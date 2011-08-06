
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
      def rootDir = project.properties['rootDir'].replace('\\', '/')
      def featureJar = "org.terracotta.dso_" + project.properties['eclipse.plugin.version']
      def destFile = rootDir + "/update/features/" + featureJar + ".jar" 
      def baseDir =  rootDir + "/update/features"
      ant.jar(destfile: destFile, basedir: baseDir)
      ant.delete(file: rootDir + "/update/features/feature.xml")
    }
    
    static void createPluginJar(project) {
      def rootDir = project.properties['rootDir'].replace('\\', '/')   
    }
}