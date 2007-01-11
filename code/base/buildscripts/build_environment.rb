#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Represents the environment that this build is running in. Adds a number of
# methods to the base Environment class that are specific to building code
# out of our source environment, as opposed to a more general Ruby or JRuby
# environment.

# Forward-declares the Environment class, so we can inherit from it.
class Environment
end

# A subclass of Environment that adds methods specific to building our code
# out of our source environment, as opposed to a more general Ruby or JRuby
# environment.
class BuildEnvironment < Environment
  include_class('java.lang.System') { |p, name| "Java" + name }

  # Creates a new instance, given a Platform object and a configuration source.
    def initialize(platform, config_source)
        super(platform)
        @config_source = config_source
        @build_timestamp = Time.now
        begin
            @svninfo = YAML::load(platform.exec("svn", "info"))        
        rescue            
            @svninfo = {}
            @svninfo["Last Changed Rev"] = "00"
            @svninfo["URL"] = "unknown-url"
        end
    end

    # What's the latest revision on the local source base?
    def current_revision
        @svninfo["Last Changed Rev"]      
    end

  # If the latest revision on the local source base is tagged, return it;
  # otherwise, return '[none]'.
  def current_revision_tag
      ""
  end

    # What branch are we building from? 
    # read from tc.build-control.branch in build-config.global
    # if not, try to parse from "svn info" command
    def current_branch
        return @branch unless @branch.blank?
        @branch = @config_source["branch"]
        if @branch.nil?
            case @svninfo["URL"]
                when /trunk/: @branch="trunk"
                when /branches\/private\/([^\/]+)\//: @branch = $1
                when /branches\/([^\/]+)\//: @branch = $1
                when /tags\/([^\/]+)\//: @branch = $1
                else @branch = "branch-unknown"
            end
        end
        @branch
    end

  
  # What host are we building on?
  def build_hostname
    hostname
  end

  # What's the name of the user we're building as?
  def username
    if @user.nil?
      @user = JavaSystem.getProperty("user.name")
    end

    @user
  end

  # Just calls #username.
  def build_username
     username
  end

  # What version are we building? This returns '[none]' if no 'version'
  # property is set in the configuration source supplied in the constructor.
  def specified_build_version
    @config_source['version'] || "[no-version]"
  end

  # When was this build started? This returns a timestamp (a Time object) that's created in
  # the constructor of this object.
  def build_timestamp
    @build_timestamp
  end

  # What's the label of the currently-running monkey? Returns 'unknown' if we're not in a
  # monkey environment.
  def monkey_label
    @config_source['monkey-name'] || 'monkey-label-unknown'
  end

  # What are the default JVM arguments we should supply to spawned programs? *Note*: this may
  # or may not actually be used in any kind of reasonably-consistent fashion. You have been
  # warned.
  def default_jvmargs
    [ ]
  end
  
  IA_LOCATION = "C:\\Program Files\\Macrovision\\InstallAnywhere 8.0 Enterprise"
  
  def has_installanywhere
    File.exist?(IA_LOCATION)
  end
end
