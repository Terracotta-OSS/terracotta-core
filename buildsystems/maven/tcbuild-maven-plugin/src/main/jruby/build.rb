# This is a Mojo wrapper around the Terracotta build files
# 
# @goal build
# @requiresDependencyResolution true
# @aggregator true
class Build < Mojo

  # @parameter type="org.apache.maven.project.MavenProject" expression="${project}"
  # @required true
  def project;;end

  # @parameter type="org.terracotta.tcbuild.configuration.TCConfigurations"
  # @required true
  def tc;;end

  # @parameter type="org.terracotta.tcbuild.configuration.ModulesDefConfiguration"
  # @required true
  def modulesDef;;end

  # @parameter type="java.util.Map"
  # @required true
  def appserver;;end

  # @parameter type="java.util.ArrayList"
  # @required true
  def goals;;end

  def execute

#    info "Building: #{$project.name}"
#    info "Basedir: #{$basedir}"

#    info "appservers:"
#    $tc.appservers.each { |as| info as.name }

#    info "buildControl:"
#    $tc.build_control.each{ |k,v| info "\t#{k}=#{v}"}

#    info "jdks:"
#    $tc.jdks.each { |jdk|
#      info "\t#{jdk.name}"
#      jdk.aliases.each { |a| info "\t\t#{a}" }
#    }

#    info "appserver:"
#    $appserver.each{ |k,v| info "\t#{k}=#{v}"}

    goals = []
    $goals.each { |goal| goals.push(goal.to_s) }

# TODO: seperate these actions into phases
    require 'build-tc'

    puts "creating BaseCodeTCBuilder"

    builder = BaseCodeTerracottaBuilder.new( goals )

    puts "running BaseCodeTCBuilder"

    builder.run
  end
end

run_mojo Build
