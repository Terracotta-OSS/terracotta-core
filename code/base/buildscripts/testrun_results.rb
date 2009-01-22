#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Knows everything there is to know about results from a test run, as needed
# at the time we run the tests. In other words, knows things like where results
# files should go, where temporary files should go, where boot JARs should go,
# and so on. It's basically the test analog of BuildResults.
#
# This is quite different, actually, from TestRunRecord, even though they have
# vaguely-similar names. TestRunRecord is all about analyzing results once they've
# already been put on disk (i.e., by running the tests); the only thing it knows
# is where the XML result files should go, not any of the other stuff this class
# knows.

class TestRunResults
  # Creates a TestRunResults object for the next free testrun directory in the
  # build-results directory (usually 'build').
  def self.next_results(build_results)
    root_dir = nil
    (1..9999).each do |seq|
      trial = FilePath.new(build_results.build_dir, "testrun-%04d" % seq)
      if ! trial.exist?
        root_dir = trial
        break
      end
    end

    unless root_dir
      raise("Unable to find an empty testing directory! We tried directories in '#@build_dir' named from " +
          "'testrun-0001' to 'testrun-9999', but they all existed (?!?).")
    end
    root_dir.ensure_directory

    # create symlink to latest testrun
    unless ENV['OS'] =~ /windows/i
      system("rm testrun-latest")
      system("ln -s -f #{root_dir.to_s} testrun-latest")
    end

    TestRunResults.new(root_dir)
  end

  # Removes all test-run results from the build-results directory by deleting all
  # test-run directories.
  def self.clean_all(build_results, ant)
    return unless File.exist?(build_results.build_dir.to_s)
    Dir.entries(build_results.build_dir.to_s).each do |entry|
      FilePath.new(build_results.build_dir, entry).delete if entry =~ /testrun-\d\d\d\d/
    end
  end

  # Creates a new instance. testrun_root_dir is the root directory where
  # results are to be stored.
  def initialize(testrun_root_dir)
    @testrun_root_dir = testrun_root_dir.to_s
  end

  # What's the root directory where results are to be stored?
  def root_dir
    @testrun_root_dir
  end

  # In what directory do various directories having to do with each subtree go?
  def subtree_dir(subtree)
    FilePath.new(@testrun_root_dir, subtree.build_module.name).ensure_directory
  end

  # In what directory should the results for a given subtree go?
  def results_dir(subtree)
    FilePath.new(subtree_dir(subtree), "#{subtree.name}.results").ensure_directory
  end

  # What's the name of the result file for a given subtree and a given test-class name?
  def results_file(subtree, test_class_name)
    FilePath.new(results_dir(subtree), "TEST-#{test_class_name}.xml")
  end

  # Where should temporary files for a given subtree be stored? This is the root of the
  # hierarchy named after fully-qualified class name that TCTestCase's getTempDir(), etc.
  # use.
  def temp_dir(subtree)
    FilePath.new(subtree_dir(subtree), "#{subtree.name}.temp").ensure_directory
  end

  # Where should Ant store its temporary files for a given subtree?
  def ant_temp_dir(subtree)
    FilePath.new(subtree_dir(subtree), "#{subtree.name}.ant-temp").ensure_directory
  end

  # Where do we store the properties that TestConfigObject uses?
  def build_configuration_file(subtree)
    FilePath.new(subtree_dir(subtree), "#{subtree.name}.config")
  end

  # In what directory should we put boot JARs that are required for the tests?
  def boot_jar_directory(subtree)
    FilePath.new(subtree_dir(subtree), "#{subtree.name}.support", "normal-boot-jars")
  end

  # Where should we store required data files for tests on this subtree?
  def tests_data_dir(subtree)
    FilePath.new(subtree_dir(subtree), "#{subtree.name}.data").ensure_directory
  end
end
