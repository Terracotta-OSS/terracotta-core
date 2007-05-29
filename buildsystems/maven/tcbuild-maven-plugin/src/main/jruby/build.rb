# expression="${component.org.apache.maven.doxia.book.BookDoxia}"

# This is a Mojo wrapper around the Terracotta build files
# 
# @goal build
# @requiresDependencyResolution true
class Build < Mojo

  # @parameter type="org.apache.maven.project.MavenProject" expression="${project}"
  # @required true
  def project;;end

  # @parameter type="org.terracotta.tcbuild.configuration.TCConfigurations"
  def tc;;end

  # @parameter type="org.terracotta.tcbuild.configuration.JdkConfiguration"
  # @required true
  def jdk;;end

  # @parameter type="org.terracotta.tcbuild.configuration.ModulesDefConfiguration"
  def modulesDef;;end

  def execute

    info "Building: #{$project.name}"

    info "Basedir: #{$basedir}"

    info "appservers:"
    $tc.appservers.each { |as| info as.name }

    info "jdk:"
    info $jdk.name
    $jdk.aliases.each { |a| info a }

    info "buildControl:"
    $tc.build_control.each{ |k,v| info "\t#{k}=#{v}"}

# TODO: seperate these actions into phases
    require 'build-tc'

#    sr = StaticResources.new( '/Users/eredmond/svn/terracotta/code/base/' )
#    info "demos dir: #{sr.demos_directory}"
  end
end

run_mojo Build