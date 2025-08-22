# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name.
project("localaiindia")

# Add the directory containing your pre-built libraries to the search path.
# The libllama.so is located in the jniLibs directory
link_directories(${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

# Create a library target for your own C++ code (e.g., native-lib.cpp)
add_library(
        localaiindia
        SHARED
        native-lib.cpp
        llama_wrapper.cpp
)

# Find and link the Android log library
find_library(
        log-lib
        log
)

# This is the crucial part: link your library against the log library AND llama.
# Because we added the path in link_directories, CMake can now find "llama".
target_link_libraries(
        localaiindia
        ${log-lib}
        # This will now correctly resolve to libllama.so in the correct ABI folder
        llama
)