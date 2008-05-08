#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
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
  attr_reader :ee_svninfo
  # Creates a new instance, given a Platform object and a configuration source.
  def initialize(platform, config_source, root_dir, ee_root_dir=nil)
    super(platform)
    @config_source = config_source
    @build_timestamp = Time.now
    @svninfo = SvnInfo.new(root_dir)
    @ee_svninfo = ee_root_dir ? SvnInfo.new(ee_root_dir) : nil
  end

  # What's the latest revision on the local source base?
  def current_revision
    @svninfo.current_revision
  end
  
  def last_changed_author
    @svninfo.last_changed_author
  end
  
  def last_changed_date
    @svninfo.last_changed_date
  end

  # What branch are we building from? 
  # read from tc.build-control.branch in build-config.global
  # if not, try to parse from "svn info" command
  def current_branch
    return @branch unless @branch.blank?
    @branch = case @svninfo.url
    when /trunk/ then "trunk"
    when /branches\/private\/([^\/]+)/ then $1
    when /branches\/([^\/]+)/ then $1
    when /tags\/([^\/]+)/ then $1
    else @config_source["branch"]
    end                    
    @branch || "unkown"
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
  def version
    return @version unless @version.nil?
    @version = @config_source['version'] || maven_version() || 'unknown'        
    @version.gsub!(/revision/, current_revision.to_s)    
    @version
  end
  
  # return maven artifacts version defined in build-config.global
  def maven_version
    @config_source['maven.version'] || 'unknown'
  end

  # Edition info: opensource or enterprise  
  def edition
    @config_source['edition'] || "opensource"
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
