# FindThrift
# --------
#
# Find thrift
#
# Find the thrift includes and library.  Once done this will define
#
#   THRIFT_INCLUDE_DIR      - where to find cthrift include, etc.
#   THRIFT_LIBRARY    - List of libraries when using thrift_base.
#   THRIFT_FOUND             - True if thrift found.
#


set(THRIFT_INCLUDE_DIR_S /usr/include)
set(THRIFT_INCLUDE_DIR_S /usr/lib64/)

find_path(THRIFT_INCLUDE_DIR NAMES thrift/ PATHS ${THRIFT_INCLUDE_DIR_S})
find_library(THRIFT_LIBRARY NAMES libthrift.a PATHS ${THRIFT_INCLUDE_DIR_S})

mark_as_advanced(THRIFT_INCLUDE_DIR THRIFT_LIBRARY)

# handle the QUIETLY and REQUIRED arguments and set MUDUO_FOUND to TRUE if
# all listed variables are TRUE
include(FindPackageHandleStandardArgs)
FIND_PACKAGE_HANDLE_STANDARD_ARGS(THRIFT REQUIRED_VARS THRIFT_INCLUDE_DIR THRIFT_LIBRARY)

MESSAGE("THRIFT_LIBRARY  ${THRIFT_LIBRARY}")
MESSAGE("THRIFT_LIBRARY_DIR  ${THRIFT_LIBRARY_DIR}")
MESSAGE("THRIFT_INCLUDE_DIR      ${THRIFT_INCLUDE_DIR}")

if(THRIFT_FOUND)
    set(THRIFT_INCLUDE_DIR ${THRIFT_INCLUDE_DIR})
    set(THRIFT_LIBRARY ${THRIFT_LIBRARY})
else()
    MESSAGE("THRIFT_FOUND NO FOUND")
endif()