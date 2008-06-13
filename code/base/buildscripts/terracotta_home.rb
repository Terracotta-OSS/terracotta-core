#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# An object that knows how to create a Terracotta home directory -- that is, a directory
# you can start the Terracotta Server in and have it actually work.
class TerracottaHome
  # Creates a new instance. directory is the directory you want to make into a
  # Terracotta home.
  def initialize(static_resources, directory)
    @static_resources = static_resources
    @directory = directory
  end
    
  # Prepares the given directory as a Terracotta home, by copying necessary files into it.
  def prepare(ant)
    @directory.ensure_directory
        
    ant.copy(:todir => @directory.to_s, :overwrite => false) {
      ant.fileset(:dir => @static_resources.terracotta_home_template_directory.to_s) {
        ant.exclude(:name => '.svn/**')
      }
    }
        
    self
  end
    
  # Where does the configuration file for this Terracotta home live?
  def config_file
    FilePath.new(@directory, 'tc-config.xml')
  end
    
  # What directory are we using?
  def dir
    @directory
  end
end