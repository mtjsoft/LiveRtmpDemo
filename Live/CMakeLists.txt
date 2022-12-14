# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)
set(distribution_DIR ${CMAKE_SOURCE_DIR}/../../../../libs)

add_library(faac
        STATIC
        IMPORTED)
set_target_properties(faac
        PROPERTIES IMPORTED_LOCATION
        ../../../../libs/${CMAKE_ANDROID_ARCH_ABI}/libfaac.a)

add_library(rtmp
        STATIC
        IMPORTED)
set_target_properties(rtmp
        PROPERTIES IMPORTED_LOCATION
        ../../../../libs/${CMAKE_ANDROID_ARCH_ABI}/librtmp.a)

add_library(x264
        STATIC
        IMPORTED)
set_target_properties(x264
        PROPERTIES IMPORTED_LOCATION
        ../../../../libs/${CMAKE_ANDROID_ARCH_ABI}/libx264.a)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

include_directories(main/cpp/include)

add_library(live
        SHARED
        src/main/cpp/AudioStream.cpp
        src/main/cpp/VideoStream.cpp
        src/main/cpp/RtmpPusher.cpp)

find_library( log-lib
        log )

target_link_libraries(live x264 faac rtmp
        -landroid
        -ljnigraphics
        ${log-lib} )


add_library( # Sets the name of the library.
        media-handle

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).

        src/main/cpp/ffmpeg/cmdutils.c
        src/main/cpp/ffmpeg/ffmpeg.c
        src/main/cpp/ffmpeg/ffmpeg_filter.c
        src/main/cpp/ffmpeg/ffmpeg_opt.c
        src/main/cpp/ffmpeg_cmd.c
        src/main/cpp/ffmpeg/ffprobe.c
        src/main/cpp/ffmpeg/ffmpeg_hw.c
        src/main/cpp/audio_player.c
        src/main/cpp/openSL_audio_player.c
        src/main/cpp/video_player.c
        src/main/cpp/ffmpeg_pusher.cpp
        src/main/cpp/AVpacket_queue.c
        src/main/cpp/media_player.c
        src/main/cpp/video_filter.c
        src/main/cpp/audio_lame.c
        src/main/cpp/fast_start.c
        src/main/cpp/ffprobe_cmd.c
        )

add_library( ffmpeg
        SHARED
        IMPORTED )
set_target_properties( ffmpeg
        PROPERTIES IMPORTED_LOCATION
        ../../../../libs/${CMAKE_ANDROID_ARCH_ABI}/libffmpeg.so )

add_library( mp3lame
        SHARED
        IMPORTED )
set_target_properties( mp3lame
        PROPERTIES IMPORTED_LOCATION
        ../../../../libs/${CMAKE_ANDROID_ARCH_ABI}/libmp3lame.so )

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

include_directories(src/main/cpp)
include_directories(src/main/cpp/include)
if(${CMAKE_ANDROID_ARCH_ABI} MATCHES "armeabi-v7a")
    include_directories(src/main/cpp/include/armeabi-v7a)
    message("This is armeabi-v7a")
elseif(${CMAKE_ANDROID_ARCH_ABI} MATCHES "arm64-v8a")
    include_directories(src/main/cpp/include/arm64-v8a)
    message("This is arm64-v8a")
endif()

find_library( # Sets the name of the path variable.
        log-lib
        log )

target_link_libraries( # Specifies the target library.
        media-handle
        mp3lame
        ffmpeg
        -landroid #native_window
        -ljnigraphics #bitmap
        -lOpenSLES #openSLES
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib} )