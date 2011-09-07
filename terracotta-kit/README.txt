Commands to build Terracotta kits:

1. Compile the whole project once:
  Run "mvn install" under "community" folder 
  
2. cd terracotta-kit
  Run one of these commands to build the desired kit:
  
  mvn install -P kit                : build OS kit
  mvn install -P enterprise,kit     : build EE kit
  mvn install -P eclipse-plugin     : build Eclipse plugin kit
  mvn install -P installer,kit      : build Installer Izpack kit

3. Additional profiles:

- "no-extra": skip packaging of ehcache, quartz, sessions, toolkit-runtime
- "tarball":  create a tar ball package
