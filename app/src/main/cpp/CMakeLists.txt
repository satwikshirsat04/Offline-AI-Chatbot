# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name.
project("localaiindia")

# Add the directory containing your pre-built libraries to the search path.
link_directories(${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

# Create a library target for your own C++ code
add_library(
        localaiindia
        SHARED
        native-lib.cpp
        llama_wrapper.cpp
        # jni_wrapper.cpp - REMOVED COMPLETELY
)

# Find and link the Android log library
find_library(
        log-lib
        log
)

# Link against the log library AND llama
target_link_libraries(
        localaiindia
        ${log-lib}
        llama
)
