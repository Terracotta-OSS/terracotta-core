module BuildData
  BUILD_DATA_FILE_NAME = 'build-data.txt'
  PATCH_DATA_FILE_NAME = 'patch-data.txt'

  # Directory where build-data and patch-data files are placed
  def build_data_dir
    FilePath.new(Registry[:build_results].classes_directory(self)).ensure_directory
  end

  # Where should we put our build-data file?
  def build_data_file
    FilePath.new(self.build_data_dir, BUILD_DATA_FILE_NAME)
  end

  # Where should we put our patch-data file?
  def patch_data_file
    FilePath.new(self.build_data_dir, PATCH_DATA_FILE_NAME)
  end

  # Creates a 'build data' file at the given location, putting into it a number
  # of properties that specify when, where, and how the code in it was compiled.
  def create_build_data(config_source, destdir = self.build_data_dir)
    create_data_file(config_source, destdir, :build_data, @build_environment.edition(@config_source['flavor']))
  end

  # Creates a 'patch data' file at the given location, putting into it a number
  # of properties that specify when, where, and how the patch in it was compiled.
  def create_patch_data(level, config_source, destdir = self.build_data_dir)
    create_data_file(config_source, destdir, :patch_data, @build_environment.edition(@config_source['flavor']), level)
  end

  def create_data_file(config_source, destdir, type, edition, level = nil)
    if type == :build_data
      keyspace    = "terracotta.build"
      output_file = FilePath.new(destdir, BUILD_DATA_FILE_NAME)
    elsif type == :patch_data
      keyspace    = "terracotta.patch"
      output_file = FilePath.new(destdir, PATCH_DATA_FILE_NAME)
    end

    File.open(output_file.to_s, "w") do |file|
      file.puts("#{keyspace}.level=#{level}") if type == :patch_data

      file.puts("#{keyspace}.productname=terracotta")
      file.puts("#{keyspace}.edition=#{edition}")
      file.puts("#{keyspace}.version=#{@build_environment.maven_version}")
      file.puts("#{keyspace}.maven.artifacts.version=#{@build_environment.maven_version}")
      file.puts("#{keyspace}.host=#{@build_environment.build_hostname}")
      file.puts("#{keyspace}.user=#{@build_environment.build_username}")
      file.puts("#{keyspace}.timestamp=#{@build_environment.build_timestamp_string}")
      file.puts("#{keyspace}.revision=#{@build_environment.os_revision}")
      file.puts("#{keyspace}.branch=#{@build_environment.current_branch}")

      # extra info if built under EE branch
      if @build_environment.is_ee_branch?
        file.puts("#{keyspace}.ee.revision=#{@build_environment.ee_revision}")
      end
    end

    output_file
  end
end
