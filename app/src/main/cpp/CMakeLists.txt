# app/src/main/cpp/CMakeLists.txt
cmake_minimum_required(VERSION 3.18)
project("theater_app_native")

# Tell CMake where our llama_cpp code lives (adjust path if needed)
add_subdirectory(
        ${CMAKE_SOURCE_DIR}/../../llama_cpp
        llama_build
)

# Build a tiny JNI shared lib that wraps llama.cpp
add_library(
        llama_jni
        SHARED
        llama_jni.cpp     # your wrapper from earlier
)

target_include_directories(llama_jni PRIVATE
        ${CMAKE_SOURCE_DIR}/../../llama_cpp
        ${CMAKE_SOURCE_DIR}/../../llama_cpp/ggml
)

# Link against the llama static lib defined by llama_cpp
target_link_libraries(llama_jni
        PUBLIC llama   # llama.cpp builds a library target named "llama"
)

# Strip exceptions/rtti for size
target_compile_options(llama_jni PRIVATE -fno-exceptions -fno-rtti)
