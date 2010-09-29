
# Utility class for building external projects in the code/base/external directory.
class ExternalProjectBuilder
  def initialize(external_projects_directory, failure_mode = :fail_fast)
    @external_projects_directory = external_projects_directory.to_s
    @failure_mode = failure_mode
  end

  def build_all
    failures = []
    for build_file in find_build_files
      begin
        build_file.build
      rescue => e
        if @failure_mode == :fail_fast
          $stderr.puts("Failed to build external: '#{build_file}'")
          raise e
        end
        failures << build_file
      end
    end
    unless failures.empty?
      if @failure_mode == :fail_at_end
        fail "The following external projects failed to build:" +
             failures.map {|f| "\n\t#{f}"} + "\nSee output for details."
      end
    end
  end

  private

  def find_build_files
    result = Array.new

    # First look for top-level aggregate build file.  This overrides any build
    # files found in any subdirectories, so if there is one we can simply
    # return it.
    if build_file = BuildFile.find_in_directory(@external_projects_directory)
      result << build_file
    else
      Dir["#{@external_projects_directory}/*"].each do |entry|
        if File.directory?(entry)
          if build_file = BuildFile.find_in_directory(entry)
            result << build_file
          end
        end
      end
    end

    result
  end

  class BuildFile

    def self.find_in_directory(dir)
      build_sh = File.join(dir, 'build.sh')
      build_rb = File.join(dir, 'build.rb')
      build_xml = File.join(dir, 'build.xml')
      pom_xml = File.join(dir, 'pom.xml')
      if File.exist?(build_sh)
        ShellBuildFile.new(build_sh)
      elsif File.exist?(build_rb)
        RubyBuildFile.new(build_rb)
      elsif File.exist?(pom_xml)
        MavenBuildFile.new(pom_xml)
      elsif File.exist?(build_xml)
        AntBuildFile.new(build_xml)
      else
        nil
      end
    end

    def build
      puts("Building external project:")
      puts("\tDirectory:  #{@directory}")
      puts("\tBuild File: #{@file_name}")
      Dir.chdir(@directory) do
        do_build
      end
    end

    def to_s
      @file
    end

    private
    def initialize(file, required_pattern)
      unless File.basename(file) =~ required_pattern
        raise "Invalid build file"
      end
      @file = file
      @file_name = File.basename(file)
      @directory = File.dirname(file)
      @platform = Registry[:platform]
    end

    def do_build
    end

    def exec(command, *args)
      result = system(command, *args)
      unless result
        raise "Command failed: #{$?}"
      end
    end
  end
  
  class ShellBuildFile < BuildFile
    def initialize(file)
      super(file, /build.sh$/)
    end

    private

    def do_build
      exec(@file)
    end
  end

  class RubyBuildFile < BuildFile
    def initialize(file)
      super(file, /build.rb$/)
    end

    private

    def do_build
      load(@file)
    end
  end

  class AntBuildFile < BuildFile
    def initialize(file)
      super(file, /build.xml$/)
    end

    private

    def do_build
      Registry[:ant].ant(:antfile => @file_name, :dir => @directory)
    end
  end

  class MavenBuildFile < BuildFile
    def initialize(file)
      super(file, /^pom\.xml$/)
    end

    private

    def do_build
      exec('mvn', '-f', @file, '-DskipTests=true', '-Dtcbuild.external=true', 'install')
    end
  end
end
