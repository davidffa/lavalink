cmake_minimum_required(VERSION 3.0)
project(lame C)

set(LAME_VERSION 3.100)

get_filename_component(ROOT_DIR "." ABSOLUTE)
set(LIBMP3LAME_SOURCE "${ROOT_DIR}/lame-${LAME_VERSION}/libmp3lame")

add_definitions(-DHAVE_STDINT_H)
add_definitions(-DUSE_FAST_LOG)
add_definitions(-DTAKEHIRO_IEEE754_HACK)

if("${CMAKE_C_COMPILER_ID}" STREQUAL "MSVC")
  set(CMAKE_C_FLAGS_RELEASE "/MT")
  add_definitions(-DUSE_LAYER_2)
  add_definitions(-DHAVE_CONFIG_H)
  add_definitions(-D_CRT_SECURE_NO_WARNINGS)
  add_definitions(-D_CRT_SECURE_NO_DEPRECATED)
  configure_file("${ROOT_DIR}/lame-${LAME_VERSION}/configMS.h" "${ROOT_DIR}/lame-${LAME_VERSION}/config.h" COPYONLY)
else()
  set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC -fdata-sections -ffunction-sections")
  add_definitions(-DSTDC_HEADERS)

  if(NOT ieee754_float32_t)
    add_definitions(-Dieee754_float32_t=float)
  endif()
endif()

file(GLOB MAIN_SOURCES "${LIBMP3LAME_SOURCE}/*.c")

include_directories(${ROOT_DIR}/lame-${LAME_VERSION}/include)
include_directories(${ROOT_DIR}/lame-${LAME_VERSION})
include_directories(${LIBMP3LAME_SOURCE})

if (WIN32)
    include_directories(${LIBMP3LAME_SOURCE}/vector)
    add_library(${PROJECT_NAME} STATIC ${MAIN_SOURCES} ${ROOT_DIR}/lame-${LAME_VERSION}/libmp3lame/vector/xmm_quantize_sub.c)
else()
    add_library(${PROJECT_NAME} STATIC ${MAIN_SOURCES})
endif()