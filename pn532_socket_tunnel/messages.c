#include <arpa/inet.h>
#include <memory.h>
#include <stdio.h>
#include <assert.h>

#include "messages.h"



int get_message_type(unsigned char *apdu, size_t apdu_len, int *msg_type, int *msg_subtype,
                     unsigned char **msg_payload, size_t *msg_payload_len) {
    if (apdu_len < 2) {
        return -1;
    }
    *msg_type = apdu[0];
    *msg_subtype = apdu[1];
    *msg_payload = apdu + MSG_HEADER_LEN;
    *msg_payload_len = apdu_len - MSG_HEADER_LEN;
    return 0;
}

void make_nfc_link_keep_alive_message(unsigned char *apdu, size_t *apdu_len) {
    *apdu_len = MSG_HEADER_LEN;
    apdu[0] = MSG_NFC;
    apdu[1] = MSG_NFC_LINK_KEEP_ALIVE;
}

int parse_socket_connect_message(unsigned char *msg, size_t msg_len, int* msg_id, unsigned short* port, char* addr) {
    if (msg_len <  sizeof(int) + sizeof(int) + sizeof(unsigned short)) {
        return -1;
    }
    *msg_id = ntohl(*((int *) msg));
    *port = ntohs(*((unsigned short *) (msg + sizeof(int))));
    int addr_len = msg_len - (sizeof(int) + sizeof(unsigned short));
    memcpy(addr, msg + sizeof(int) + sizeof(unsigned short), addr_len);
    addr[addr_len] = 0;
    return 0;
}

void make_socket_connect_response(unsigned char *command_apdu, size_t *command_apdu_len, int in_reply_to,
                                  int res_or_fd) {
    *command_apdu_len = MSG_HEADER_LEN + sizeof(int) + sizeof(int);
    command_apdu[0] = MSG_SOCKET;
    command_apdu[1] = MSG_SOCKET_CONNECT_RESPONSE;

    int *ptr = (int *) (command_apdu + MSG_HEADER_LEN);
    *ptr++ = htonl(in_reply_to);
    *ptr = htonl(res_or_fd);
}

int parse_socket_recv_message(unsigned char *msg, size_t msg_len, int* msg_id, int* fd, int* len) {
    if (msg_len < sizeof(int) + sizeof(int) + sizeof(int)) {
        return -1;
    }
    int *ptr = (int *)msg;
    *msg_id = ntohl(*ptr++);
    *fd = ntohl(*ptr++);
    *len = ntohl(*ptr);
    return 0;
}

void make_socket_recv_response(unsigned char *command_apdu, size_t *command_apdu_len, int in_reply_to, int res,
                               unsigned char *data) {
    if (res > 0) {
        *command_apdu_len = MSG_HEADER_LEN + sizeof(int) + sizeof(int) + res;
    } else {
        *command_apdu_len = MSG_HEADER_LEN + sizeof(int) + sizeof(int);
    }
    command_apdu[0] = MSG_SOCKET;
    command_apdu[1] = MSG_SOCKET_RECV_RESPONSE;

    int *ptr = (int *) (command_apdu + MSG_HEADER_LEN);
    *ptr++ = htonl(in_reply_to);
    *ptr++ = htonl(res);
    if (res > 0) {
        memcpy(ptr, data, res);
    }
}

int parse_socket_send_message(unsigned char *msg, size_t msg_len, int* msg_id, int* fd, int* len, unsigned char **data) {
    if (msg_len < sizeof(int) + sizeof(int)) {
        return -1;
    }
    int *ptr = (int *)msg;
    *msg_id = ntohl(*ptr++);
    *fd = ntohl(*ptr++);
    *len = msg_len - (sizeof(int) + sizeof(int));
    *data = (unsigned char*)ptr;
    return 0;
}

void make_socket_send_response(unsigned char *command_apdu, size_t *command_apdu_len, int in_reply_to, int res) {
    *command_apdu_len = MSG_HEADER_LEN + sizeof(int) + sizeof(int);
    command_apdu[0] = MSG_SOCKET;
    command_apdu[1] = MSG_SOCKET_SEND_RESPONSE;
    int *ptr = (int *) (command_apdu + MSG_HEADER_LEN);
    *ptr++ = htonl(in_reply_to);
    *ptr = htonl(res);
}

int parse_socket_close_message(unsigned char *msg, size_t msg_len, int *msg_id, int *fd) {
    if (msg_len < sizeof(int) + sizeof(int)) {
        return -1;
    }
    int *ptr = (int *)msg;
    *msg_id = ntohl(*ptr++);
    *fd = ntohl(*ptr);
    return 0;
}

void make_socket_close_response(unsigned char *command_apdu, size_t *command_apdu_len, int in_reply_to, int res) {
    *command_apdu_len = MSG_HEADER_LEN + sizeof(int) + sizeof(int);
    command_apdu[0] = MSG_SOCKET;
    command_apdu[1] = MSG_SOCKET_CLOSE_RESPONSE;
    int *ptr = (int *) (command_apdu + MSG_HEADER_LEN);
    *ptr++ = htonl(in_reply_to);
    *ptr = htonl(res);
}
