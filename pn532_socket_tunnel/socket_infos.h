#ifndef PN532_SOCKET_TUNNEL_SOCKET_INFOS_H
#define PN532_SOCKET_TUNNEL_SOCKET_INFOS_H

#include <pthread.h>

typedef struct socket_info {
    int fd;

    // statistics
    int total_bytes_read;
    int total_bytes_sent;

    // references to worker threads
    pthread_t thread_send;
    pthread_t thread_recv;

    // pending send data
    int pending_send;
    unsigned char *pending_send_data;
    int pending_send_data_len;
    int *pending_send_res; // result of the last pending send, NULL if no result is pending

    // pending recv data
    int pending_recv;
    unsigned char *pending_recv_data;
    int pending_recv_data_len;
    int *pending_recv_res;  // result of the last pending recv, NULL if no result is pending

} socket_info;

void init_socket_infos();

void release_socket_infos();

struct socket_info *find_socket_info(int fd);

struct socket_info *find_free_socket_info();

struct socket_info *find_active_socket_info();

void free_socket_info(int fd);

struct socket_info *find_pending_recv_socket_info();

struct socket_info *find_pending_send_socket_info();

#endif //PN532_SOCKET_SOCKET_INFOS_H
