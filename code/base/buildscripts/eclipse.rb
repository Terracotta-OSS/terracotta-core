#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class EclipseProjectBuilder 

  def initialize(singleProject, ant) 
    @singleProject = singleProject
    @ant = ant
    initializeXMLBuilder
  end

  def initializeXMLBuilder
    @xmlBuilder = XMLBuilder.new("classpath")
    @xmlBuilder.root.element("classpathentry").attribute("kind", "output").attribute("path", "build.eclipse/ECLIPSE.IS.MESSEDUP")
    @xmlBuilder.root.element("classpathentry").attribute("kind", "con").attribute("path", "org.eclipse.jdt.launching.JRE_CONTAINER")
  end

  def addSrcDir(srcDir) 
    @xmlBuilder.root.element("classpathentry").attribute("excluding", "**/.svn/**").attribute("output", "build.eclipse/#{prefix(@currentModule.name)}#{srcDir}.classes")		.attribute("kind", "src")		.attribute("path", "#{prefix(@currentModule.name)}#{srcDir}")
  end

  def addResourceDir(srcDir) 
    @xmlBuilder.root.element("classpathentry").attribute("excluding", "**/.svn/**").attribute("output", "build.eclipse/#{prefix(@currentModule.name)}#{srcDir}.classes").attribute("kind", "src").attribute("path", "#{prefix(@currentModule.name)}#{srcDir}.resources")
  end

  def addJarDir(name, jarDir) 
    Dir.foreach(jarDir.to_s) do |filename|
      if filename.ends_with?(".jar")
        addJar("#{prefix(@currentModule.name)}lib#{name}/#{filename}")
      end
    end
  end

  def processModule(myModule)
    @currentModule = myModule;
  end

  def completeModule
    unless @singleProject
      addDependentProjects(@currentModule.dependencies)
      generateClasspathAndProject(@currentModule.name)
      initializeXMLBuilder
    end
  end 

  def prefix(thePrefix) 
    if @singleProject 
      return thePrefix + "/"
    else
      ""
    end
  end


  def addJar(name) 
    @xmlBuilder.root.element("classpathentry").attribute("kind", "lib").attribute("path", name).attribute("exported", "true")

  end

  def generate 
    puts "Generating: " + File.expand_path(".classpath")
    File.open(".classpath", "w") { |file| file << @xmlBuilder.to_s}
  end

  def addDependentProjects(dependencies)
    dependencies.each {|dependent| @xmlBuilder.root.element("classpathentry").attribute("combineaccessrules", "false").attribute("kind", "src").attribute("path", "/" + dependent) }
  end

  def generateClasspathAndProject(moduleName)
    generateClasspath(moduleName)
    generateProject(moduleName)
    generateSettings(moduleName)
  end

  def generateClasspath(moduleName) 
    classpathFile = moduleName + "/.classpath"
    puts "Generating: " + File.expand_path(classpathFile)
    File.open(classpathFile, "w") { |file| file << @xmlBuilder.to_s}
  end
    
  def generateProject(moduleName)
    projectPath = moduleName + "/.project"
    unless File.exists?(projectPath)
      xmlBuilder = XMLBuilder.new("projectDescription")
      xmlBuilder.root.elementWithContext("name", moduleName)
      xmlBuilder.root.elementWithContext("comment", moduleName + " source code")
      xmlBuilder.root.element("projects")
      buildSpecs = xmlBuilder.root.element("buildSpec")
      addBuilder(buildSpecs, "org.eclipse.jdt.core.javabuilder")
      natures = xmlBuilder.root.element("natures")
      addNature(natures, "org.eclipse.jdt.core.javanature")
      addNature(natures, "org.eclipse.pde.PluginNature")
      puts "Generating: " + File.expand_path(moduleName + "/.project")
      File.open(projectPath, "w") { |file| file << xmlBuilder.to_s}
    end
  end
    
  def generateSettings(moduleName) 
    settingsDirPath = moduleName + "/" + ".settings"
    unless File.directory?(settingsDirPath)
      Dir.mkdir settingsDirPath
      @ant.copy(:todir => settingsDirPath) {@ant.fileset(:dir => ".settings") }
    end 
  end

  def addBuilder(builderSpecs, builder)
    builderElement = builderSpecs.element("buildCommand")
    builderElement.elementWithContext("name", builder)
    builderElement.element("arguments")
  end

  def addNature(natures, nature)
    natures.elementWithContext("nature", nature)
  end

end

class XMLBuilder  
  def initialize(rootElementName)
    @elements = []
    @rootElement = ElementBuilder.new(rootElementName)
  end

  def root
    return @rootElement
  end

  def to_s
    '<?xml version="1.0" encoding="UTF-8"?>' + "\n" + @rootElement.to_s
  end

end

class ElementBuilder
  def initialize(elementName, content = nil)
    @name = elementName
    @elements = []
    @attributes = {}
    @content = content
  end

  def attribute(name, value) 
    @attributes[name] = value
    return self
  end

  def element(elementName) 
    element = ElementBuilder.new(elementName)
    @elements << element
    return element
  end

  def elementWithContext(elementName, content)
    element = ElementBuilder.new(elementName, content)
    @elements << element
    return element
  end

  def to_s
    result = "<#{@name} #{attributes_to_s}"
    if @elements.empty? && @content == nil
      result + "/>"
    else
      result << ">"
      if @content != nil 
        result << @content
      end
      @elements.each {|child| result = result + child.to_s + "\n"}
      result +  "</#{@name}>\n"
    end
  end

  def attributes_to_s 
    result = ""
    @attributes.each {|name, value| result = result + name + '="' + value + '" '}
    result 
  end

end
