def ant = new AntBuilder()
ant.taskdef(name: "izpack", classname: "com.izforge.izpack.ant.IzPackTask")

ant.izpack(input: project.properties['izpack.install.xml'],
           output: project.properties['izpack.installer.output'],
           basedir: project.properties['izpack.basedir'],
           installerType: "standard",
           inheritAll: true,
           compression: "deflate",
           compressionLevel: 9)
