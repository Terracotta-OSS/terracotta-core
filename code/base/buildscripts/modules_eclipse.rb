#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BuildSubtree
  def eclipse(build_environment, eclipseBuilder)
    if @source_exists
      eclipseBuilder.addSrcDir(@name)
    end
    if @resources_exists
      eclipseBuilder.addResourceDir(@name)
    end

    jardir = FilePath.new(@build_module.root, "lib" + lib_suffix)
    if FileTest.directory?(jardir.to_s) 
      eclipseBuilder.addJarDir(lib_suffix, jardir)
    end
  end
end

class BuildModule
  def eclipse(environment, eclipseBuilder)
    eclipseBuilder.processModule(self)
    @subtrees.each { |subtree| subtree.eclipse(environment, eclipseBuilder) }
    eclipseBuilder.completeModule()
  end

  #    def meclipse(environment)
  #        eclipseBuilder = EclipseProjectBuilder.new(false)
  #        eclipseBuilder.addDependentProjects(@dependencies)
  #         @subtrees.each { |subtree| subtree.eclipse(environment, eclipseBuilder) }
  #        eclipseBuilder.generateClasspathAndProject(@name)
  #   end
end