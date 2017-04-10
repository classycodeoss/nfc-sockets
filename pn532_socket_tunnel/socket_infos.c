#include <stdlib.h>
#include <stdio.h>
#include <strings.h>

#include "socket_infos.h"

#define SOCKET_FD_INVALID -1
#define NUM_SOCKETS 32

socket_info socket_infos[NUM_SOCKETS];


void init_socket_infos() {
    bzero(socket_infos, sizeof(socket_infos));
    for (int i = 0; i < NUM_SOCKETS; i++) {
        socket_infos[i].fd = SOCKET_FD_INVALID;
    }
}

struct socket_info *find_socket_info(int fd) {
    for (int i = 0; i < NUM_SOCKETS; i++) {
        if (socket_infos[i].fd == fd) {
            return &socket_infos[i];
        }
    }
    return NULL;
}

struct socket_info *find_active_socket_info() {
    for (int i = 0; i < NUM_SOCKETS; i++) {
        if (socket_infos[i].fd != SOCKET_FD_INVALID) {
            return &socket_infos[i];
        }
    }
    return NULL;
}

struct socket_info *find_free_socket_info() {
    return find_socket_info(SOCKET_FD_INVALID);
}

void free_socket_info(int fd) {
    struct socket_info *si = find_socket_info(fd);
    if (si != NULL) {
        printf("Freeing socket info for fd=%d (total bytes sent/recvd = %d/%d)\n",
               fd, si->total_bytes_sent, si->total_bytes_read);
        if (si->pending_recv_res) {
            fprintf(stderr, "Discarding pending recv result for socket: %d\n", fd);
            free(si->pending_recv_res);
        }
        if (si->pending_send_res) {
            fprintf(stderr, "Discarding pending send result for socket: %d\n", fd);
            free(si->pending_send_res);
        }
        bzero(si, sizeof(struct socket_info));
        si->fd = SOCKET_FD_INVALID;
    }
}

struct socket_info *find_pending_recv_socket_info() {
    for (int i = 0; i < NUM_SOCKETS; i++) {
        if (socket_infos[i].fd != SOCKET_FD_INVALID && socket_infos[i].pending_recv_res != NULL) {
            return &socket_infos[i];
        }
    }
    return NULL;
}

struct socket_info *find_pending_send_socket_info() {
    for (int i = 0; i < NUM_SOCKETS; i++) {
        if (socket_infos[i].fd != SOCKET_FD_INVALID && socket_infos[i].pending_send_res != NULL) {
            return &socket_infos[i];
        }
    }
    return NULL;
}