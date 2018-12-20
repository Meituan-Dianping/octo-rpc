# FindMuduo
# --------
#
# Find Muduo
#
# Find the Muduo includes and library.  Once done this will define
#
#   MUDUO_INCLUDE_DIR      - where to find gtest include, etc.
#   MUDUO_LIBRARY          - List of libraries when using muduo_base.
#   MUDUO_FOUND            - True if muduo found.
#



set(MUDUO_INCLUDE_DIR_S /usr/local/include)
set(MUDUO_LIBRARY_S /usr/local/lib/)

find_path(MUDUO_INCLUDE_DIR NAMES muduo PATHS ${MUDUO_INCLUDE_DIR_S})
find_library(MUDUO_LIBRARY NAMES libmuduo_base.a PATHS ${MUDUO_LIBRARY_S})

mark_as_advanced(MUDUO_LIBRARY MUDUO_INCLUDE_DIR)

# handle the QUIETLY and REQUIRED arguments and set MUDUO_FOUND to TRUE if
# all listed variables are TRUE
include(FindPackageHandleStandardArgs)
FIND_PACKAGE_HANDLE_STANDARD_ARGS(MUDUO REQUIRED_VARS MUDUO_LIBRARY MUDUO_INCLUDE_DIR)

MESSAGE("MUDUO_INCLUDE_DIR  ${MUDUO_INCLUDE_DIR}")
MESSAGE("MUDUO_LIBRARY      ${MUDUO_LIBRARY}")

if(MUDUO_FOUND)
    set(MUDUO_INCLUDE_DIR ${MUDUO_INCLUDE_DIR})
    set(MUDUO_LIBRARY ${MUDUO_LIBRARY})
else()
    MESSAGE("MUDUO_FOUND NO FOUND Download and compile")
    include(DonwloadMuduo)
endif()