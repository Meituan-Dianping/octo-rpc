# FindGtest
# --------
#
# Find gtest
#
# Find the gtest includes and library.  Once done this will define
#
#   GTEST_INCLUDE_DIR      - where to find gtest include, etc.
#   GTEST_LIBRARY    - List of libraries when using gtest_base.
#   GTEST_FOUND             - True if gtest found.
#
set(GTEST_INCLUDE_DIR ${COMMON_LIB_PATH}/googletest/googletest/include)
set(GTEST_LIBRARY ${COMMON_LIB_PATH}/googletest/googlemock/gtest/libgtest.a ${COMMON_LIB_PATH}/googletest/googlemock/gtest/libgtest_main.a)
set(GTEST_MOCK_LIBRARY ${COMMON_LIB_PATH}/googletest/googlemock/libmock.a ${COMMON_LIB_PATH}/googletest/googlemock/gtest/libmock_main.a)

#message(${GTEST_INCLUDE_DIR})
find_path(GTEST_INCLUDE_DIR NAMES gtest)

mark_as_advanced(GTEST_LIBRARY GTEST_MOCK_LIBRARY GTEST_INCLUDE_DIR)

# handle the QUIETLY and REQUIRED arguments and set GTEST_FOUND to TRUE if
# all listed variables are TRUE
include(FindPackageHandleStandardArgs)
FIND_PACKAGE_HANDLE_STANDARD_ARGS(Gtest REQUIRED_VARS GTEST_LIBRARY GTEST_MOCK_LIBRARY GTEST_INCLUDE_DIR)

if(GTEST_FOUND)
  set(GTEST_INCLUDE_DIR ${GTEST_INCLUDE_DIR})
  set(GTEST_LIBRARY ${GTEST_LIBRARY})
  set(GTEST_MOCK_LIBRARY ${GTEST_MOCK_LIBRARY})
endif()

