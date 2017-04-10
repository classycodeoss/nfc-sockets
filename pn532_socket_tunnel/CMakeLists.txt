cmake_minimum_required(VERSION 3.6)
project(pn532_socket_tunnel)

set(CMAKE_C_STANDARD 11)

find_path(NFC_INCLUDE nfc/nfc.h)
find_library(NFC_LIBRARY NAMES nfc)
include_directories(${NFC_INCLUDE})
find_library(PTHREAD_LIBRARY NAMES pthread)

set(SOURCE_FILES_SOCKET_TUNNEL pn532_socket_tunnel.c socket_infos.c messages.c)
add_executable(pn532_socket_tunnel ${SOURCE_FILES_SOCKET_TUNNEL})
target_link_libraries(pn532_socket_tunnel ${NFC_LIBRARY} ${PTHREAD_LIBRARY})
