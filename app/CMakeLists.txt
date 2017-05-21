# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)

# [EDIT] Set Path to OpenCV and include the directories
# pathToOpenCV is just an example to how to write in Mac.
# General format: /Users/user-name/OpenCV-android-sdk/sdk/native
# /opt/OpenCV-android-sdk-v3.1.0/sdk/native/jni/include

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             MyMensor

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             # Associated headers in the same location as their source
             # file are automatically included.
             src/main/cpp/MyMensorJNI.cpp
             src/main/cpp/IdMarkerDetectionFilter.cpp
             src/main/cpp/ImageDetectionFilter.cpp
             src/main/cpp/VpConfigureFilter.cpp)

# [EDIT] Similar to above lines, add the OpenCV library
include_directories(/opt/OpenCV-android-sdk-v3.1.0/sdk/native/jni/include)
add_library( lib_opencv SHARED IMPORTED )
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION /Users/jorgercg/projects/and/MyMensor/app/src/main/jniLibs/${ANDROID_ABI}/libopencv_java3.so)

add_library( lib_opencvaruco SHARED IMPORTED )
set_target_properties(lib_opencvaruco PROPERTIES IMPORTED_LOCATION /Users/jorgercg/projects/and/MyMensor/app/src/main/jniLibs/${ANDROID_ABI}/libopencv_aruco.so)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because system libraries are included in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in the
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       MyMensor

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} lib_opencv lib_opencvaruco)
