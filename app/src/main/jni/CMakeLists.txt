cmake_minimum_required(VERSION 3.10.2)

project("MySynth")

# Note:  Later android ndk's don't build libraries compatible with
# older libraries, so we're avoiding creating the MySynthOpenSL library.
# Instead, a version linked with ndk version 19 is pre=built and
# checked into jniLibs.  If you need to rebuild it, you should temporarily
# downgrade to minsdk 19 but you'll also need to comment out
# the building of MySynthAAudio (since the aaudio library was not
# available in version 19.
#
#add_library(
#        MySynthOpenSL
#        SHARED
#        MySynthOpenSL.c )
#
#find_library(log-lib log)
#find_library(opensl-lib OpenSLES)
#
#target_link_libraries(
#        MySynthOpenSL
#        ${log-lib}
#        ${opensl-lib})

add_library(
        MySynthAAudio
        SHARED
        MySynthAAudio.c )

find_library(log-lib log)
find_library(aaudio-lib  aaudio)

target_link_libraries(
        MySynthAAudio
        ${log-lib}
        ${aaudio-lib})
