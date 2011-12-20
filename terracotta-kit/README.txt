Commands to build Terracotta kits:

1. Compile the whole project once:
  Run "mvn install" under "community" folder 
  
2. cd terracotta-kit
  Run one of these commands to build the desired kit:
  
  mvn install -P kit                : build OS kit
  mvn install -P enterprise,kit     : build EE kit
  mvn install -P enterprise,gt,kit  : build EE -gt kit
  mvn install -P installer,kit      : build Installer Izpack kit

3. To create a patched kit
  First, edit the src/packaging/patch/patch-def.xml file to define the contents
  of the patch, then run:

  mvn install -P kit,patch

  This will create a terracotta-<VERSION>-patch<LEVEL>.tar.gz file in the target
  directory, where <VERSION> is the version of the kit and <LEVEL> is the patch
  level specified in the patch-def.xml file.

4. Additional profiles:

- "no-extra": skip packaging of ehcache, quartz, sessions, toolkit-runtime
- "tarball":  create a tar ball package
