#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Unit tests for BuildModule. These should be augmented a great, *great*, GREAT, *GREAT* deal. :)

require 'test/unit'
require 'modules'
require 'environment'

module Modules
    class Tests < Test::Unit::TestCase
        def test_creation
            assert_raise(RuntimeError) { Modules::BuildModule.new(:root => FilePath.new(Dir.getwd, "../foobar").to_s, :compiler_version => '1.4') }
            assert_raise(RuntimeError) { Modules::BuildModule.new(:root => "/foewrwi", :compiler_version => '1.4') }
        end
        
        def test_subtrees
            mod = Modules::BuildModule.new(:root => FilePath.new(Dir.getwd, "../common").to_s, :compiler_version => '1.4')
            puts mod[:src].classpath([ ], :full, :runtime)
            
            build_environment = BuildEnvironment.new(:build_directory => '../build')
            
            mod[:src].compile(build_environment)
        end
    end
end