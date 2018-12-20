cmake_minimum_required(VERSION 2.6)

function(cthrift_add_test test_name command_line)
   # message("argv=${ARGV}, argn=${ARGN}")
    add_test(${test_name} ${command_line} ${ARGN})
    # add_test(NAME ${test_name} COMMAND ${command_line} ${ARGN})
    #set_tests_properties(${test_name} PROPERTIES ENVIRONMENT)
endfunction(cthrift_add_test)

