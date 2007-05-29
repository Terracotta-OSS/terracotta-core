#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Unit tests for the FilePath class. Very, very incomplete.

require 'test/unit'
require 'paths'
require 'additions'

class FilePathTests < Test::Unit::TestCase
    def test_file_path
        data = {
            [ ] => [ "", false ],
            [ "" ] => [ "", false ],
            [ "a" ] => [ "a", false ],
            [ "  a  " ] => [ "  a  ", false ],
            [ "/a" ] => [ "/a", true ],
            [ "a/" ] => [ "a", false ],
            [ "//a" ] => [ "/a", true ],
            [ "a", "b" ] => [ "a/b", false ],
            [ "  a ", "  b  " ] => [ "  a /  b  ", false ],
            [ "a/", "b" ] => [ "a/b", false ],
            [ "a//", "b" ] => [ "a/b", false ],
            [ "a/", "/b" ] => [ "a/b", false ],
            [ "//a//", "//b//" ] => [ "/a/b", true ],
            [ "/" ] => [ "/", true ],
            [ "/", "a" ] => [ "/a", true ],
            [ "/", "/", "/" ] => [ "/", true ],
            [ "/////", "////" ] => [ "/", true ],
            [ "/", "", "", "/a", "//", "/", "b" ] => [ "/a/b", true ],
            [ "a", "", "", "", "b" ] => [ "a/b", false ],
            [ "a", "", "  ", "", "b" ] => [ "a/b", false ],
            
            [ "a/b" ] => [ "a/b", false ],
            [ "a/b", "c" ] => [ "a/b/c", false ],
            [ "a\\b" ] => [ "a/b", false ],
            [ "a\\b", "c" ] => [ "a/b/c", false ],
            [ "\\a/b" ] => [ "/a/b", true ],
            [ "/a\\b" ] => [ "/a/b", true ],
        }
        
        data.each do |components,expected_array|
            expected_string, expected_absolute = expected_array
            
            path = FilePath.new(*components)
            assert_equal(expected_string, path.to_s, "FilePath.new(#{components.join("|")})")
            assert_equal(expected_absolute, path.absolute?)
            
            path = FilePath.new
            components.each { |component| path << component }
            assert_equal(expected_string, path.to_s, "FilePath.new(#{components.join("|")})")
            assert_equal(expected_absolute, path.absolute?)
        end
    end
    
    def test_path_set
        data = {
            [ ] => "",
            [ "" ] => "",
            [ "a" ] => "a",
            [ "a", "b" ] => "a:b",
            [ "a:b" ] => "a:b",
            [ "a::b" ] => "a:b",
            [ "a:", "b" ] => "a:b",
            [ "a:", ":", ":b" ] => "a:b",
            [ "::::" ] => "",
            [ "a", "", "", "b" ] => "a:b",
            [ "a;b", ";;c" ] => "a:b:c",
        }
        
        data.each do |components,expected|
            set = PathSet.new(*components)
            assert_equal(expected, set.to_s)
        end
    end
end
