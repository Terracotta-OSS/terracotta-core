#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

require 'tmpdir'
require 'fileutils'

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  protected

  # - extract hibernate referecne config from standalone agent jar
  def postscript(ant, build_environment, product_directory, *args)
    return if @no_demo

    # first and only argument is a hash
    arg = args[0]

    dest = File.join(product_directory.to_s, arg['dest'])
    agent_pattern = arg['agent-pattern']
    provider_pattern = arg['provider-pattern']
    reference_file = arg['reference-file']
    work_dir = File.join(Dir.tmpdir, "tcbuild-extract")

    # look up the agent jar
    agent_jar = nil
    Dir.chdir(dest) do |path|
      Dir.glob(agent_pattern + "*.jar") { |filename|
        agent_jar = File.join(path, filename)
      }
      fail("Can't find agent jar with pattern #{agent_pattern}") unless File.exists?(agent_jar)
    end

    # extract provider from agent jar
    ant.unzip(:src => agent_jar, :dest => work_dir) do
      ant.patternset(:includes => "**/" + provider_pattern + "*.jar")
    end

    # look up provider jar
    provider_jar = nil
    Dir.chdir(File.join(work_dir, "TIMs")) do |path|
      Dir.glob(provider_pattern + "*.jar") { |filename|
        provider_jar = File.join(path, filename)
      }
      fail("Can't find provider jar with pattern #{provider_pattern}") unless File.exists?(provider_jar)
    end

    # extract reference-config.xml from provider jar
    ant.unzip(:src => provider_jar, :dest => work_dir) do
      ant.patternset(:includes => reference_file)
    end

    ref_path = File.join(work_dir, reference_file)
    fail("Reference config is not found #{ref_path}") unless File.exists?(ref_path)
    
    # copy it over to dest
    FileUtils.cp ref_path, dest

    # clean up
    FileUtils.rm_rf(work_dir)
  end
end
