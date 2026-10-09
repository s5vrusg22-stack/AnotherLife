cmake_minimum_required(VERSION 3.22.1)
project(anotherlife_llama LANGUAGES C CXX)
set(CMAKE_CXX_STANDARD 17)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
if(NOT EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/third_party/llama.cpp/CMakeLists.txt")
 message(FATAL_ERROR "llama.cpp source checkout missing")
endif()
add_subdirectory(third_party/llama.cpp EXCLUDE_FROM_ALL)
# Compile against the exact checked-out llama.cpp API and expose readable
# failures rather than shipping a Kotlin-only APK without JNI.
add_library(anotherlife_llama SHARED llama_jni.cpp)
target_compile_features(anotherlife_llama PRIVATE cxx_std_17)
target_include_directories(anotherlife_llama PRIVATE
 "${CMAKE_CURRENT_SOURCE_DIR}/third_party/llama.cpp/include"
 "${CMAKE_CURRENT_SOURCE_DIR}/third_party/llama.cpp/ggml/include")

target_link_libraries(anotherlife_llama PRIVATE llama android log)
