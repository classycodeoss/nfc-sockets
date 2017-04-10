

#ifndef PN532_SOCKET_TUNNEL_MESSAGES_H
#define PN532_SOCKET_TUNNEL_MESSAGES_H

#include <stdlib.h>

// General message constants
#define MSG_HEADER_LEN 2

// Responses to socket messages

#define MSG_SOCKET_CONNECT_RESPONSE 0
#define MSG_SOCKET_SEND_RESPONSE 1
#define MSG_SOCKET_RECV_RESPONSE 2
#define MSG_SOCKET_CLOSE_RESPONSE 3

// Received message types
#define MSG_NFC 0
#define MSG_SOCKET 1

// NFC messages
#define MSG_NFC_LINK_KEEP_ALIVE 0
#define MSG_NFC_LINK_TERMINATE 1

// Socket messages
#define MSG_SOCKET_CONNECT 0
#define MSG_SOCKET_SEND 1
#define MSG_SOCKET_RECV 2
#define MSG_SOCKET_CLOSE 3


// General

int get_message_type(unsigned char *apdu, size_t apdu_len, int *msg_type, int *msg_subtype,
                     unsigned char **msg_payload, size_t *msg_payload_len);

// NFC messages

void make_nfc_link_keep_alive_message(unsigned char *apdu, size_t *apdu_len);

// Socket messages

int parse_socket_connect_message(unsigned char *msg, size_t msg_len, int *msg_id, unsigned short *port, char *addr);

void make_socket_connect_response(unsigned char *apdu, size_t *apdu_len, int in_reply_to, int res_or_fd);

int parse_socket_recv_message(unsigned char *msg, size_t msg_len, int *msg_id, int *fd, int *len);

void make_socket_recv_response(unsigned char *apdu, size_t *apdu_len, int in_reply_to, int res, unsigned char *data);

int parse_socket_send_message(unsigned char *msg, size_t msg_len, int *msg_id, int *fd, int *len, unsigned char **data);

void make_socket_send_response(unsigned char *apdu, size_t *apdu_len, int in_reply_to, int res);

int parse_socket_close_message(unsigned char *msg, size_t msg_len, int *msg_id, int *fd);

void make_socket_close_response(unsigned char *apdu, size_t *apdu_len, int in_reply_to, int res);

#endif //PN532_SOCKET_TUNNEL_MESSAGES_H
