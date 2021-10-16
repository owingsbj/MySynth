cmake_minimum_required(VERSION 3.10.2)

project("MySynth")

add_library(
        MySynthOpenSL
        SHARED
        MySynthOpenSL.c )

add_library(
        MySynthAAudio
        SHARED
        MySynthAAudio.c )

find_library(log-lib log )
find_library(opensl-lib OpenSLES)
find_library(aaudio-lib  aaudio)

target_link_libraries(
        MySynthOpenSL
        ${log-lib}
        ${opensl-lib})

target_link_libraries(
        MySynthAAudio
        ${log-lib}
        ${aaudio-lib})
