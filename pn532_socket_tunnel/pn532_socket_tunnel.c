#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <assert.h>
#include <pthread.h>

#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>

#include <nfc/nfc.h>

#include "messages.h"
#include "socket_infos.h"

// By default we expect the PN532 to be connected via SPI
#define DEFAULT_CONN_STRING "pn532_spi:/dev/spidev0.0:1000000"

// Default timeout for APDU transmissions
#define TX_TIMEOUT 1000

// Constants
#define MSG_ID_ILLEGAL 0

int card_transmit(nfc_device *pnd,
                  uint8_t *command_apdu, size_t command_apdu_len,
                  uint8_t *response_apdu, size_t *response_apdu_len) {
    int res = nfc_initiator_transceive_bytes(pnd, command_apdu, command_apdu_len, response_apdu, *response_apdu_len,
                                             TX_TIMEOUT);
    if (res < 0) {
        return -1;
    } else {
        *response_apdu_len = (size_t) res;
        return 0;
    }
}

int
handle_socket_connect_message(uint8_t *message, size_t message_len, uint8_t *command_apdu, size_t *command_apdu_len) {

    int msg_id;
    unsigned short port;
    char addr_buf[1024];

    if (parse_socket_connect_message(message, message_len, &msg_id, &port, addr_buf) != 0) {
        fprintf(stderr, "Failed to parse SOCKET_CONNECT message\n");
        return -1;
    }

    // allocate socket info
    struct socket_info *si = find_free_socket_info();
    if (si == NULL) {
        fprintf(stderr, "Number of sockets exhausted\n");
        return -1;
    }

    // allocate socket
    int fd = socket(AF_INET, SOCK_STREAM, 0);
    if (fd >= 0) {
        printf("New socket allocated: %d\n", fd);
        si->fd = fd;
    } else {
        fprintf(stderr, "Failed to allocate socket: %d\n", fd);
        make_socket_connect_response(command_apdu, command_apdu_len, msg_id, fd);
        return 0;
    }

    // connect socket
    struct hostent *host_addr = gethostbyname(addr_buf);
    if (host_addr == NULL) {
        fprintf(stderr, "Failed to look up host: %s\n", addr_buf);
        return -1;
    }
    struct sockaddr_in sa;
    memset(&sa, 0, sizeof sa);
    sa.sin_family = AF_INET;
    sa.sin_port = htons(port);
    memcpy(&sa.sin_addr.s_addr, host_addr->h_addr, host_addr->h_length);

    int res = connect(fd, (struct sockaddr *) &sa, sizeof(sa));
    if (res == 0) {
        printf("socket %d connected to %s:%d\n", fd, addr_buf, port);
        make_socket_connect_response(command_apdu, command_apdu_len, msg_id, fd);
    } else {
        fprintf(stderr, "socket %d connection failed %s:%d\n", fd, addr_buf, port);
        make_socket_connect_response(command_apdu, command_apdu_len, msg_id, res);
    }
    return 0;
}

void *recv_socket(void *arg) {
    socket_info *si = (socket_info *) arg;
    int fd = si->fd;

    int res = recv(fd, si->pending_recv_data, si->pending_recv_data_len, 0);
    printf("[recv_worker] socket.recv(%d, %d) = %d\n", fd, si->pending_recv_data_len, res);

    // set result
    si->pending_recv_res = malloc(sizeof(int));
    *(si->pending_recv_res) = res;

    // update stats
    if (res > 0) {
        si->total_bytes_read += res;
    }

    return NULL;
}

int handle_socket_recv_message(uint8_t *message, size_t message_len) {
    // parse message
    int msg_id;
    int fd;
    int len;
    if (parse_socket_recv_message(message, message_len, &msg_id, &fd, &len) != 0) {
        fprintf(stderr, "Failed to parse SOCKET_RECV message\n");
        return -1;
    }
    assert(len > 0);

    // find corresponding socket
    struct socket_info *si = find_socket_info(fd);
    if (si == NULL) {
        fprintf(stderr, "Unknown socket: %d\n", fd);
        return -1;
    }
    if (si->pending_recv != MSG_ID_ILLEGAL) {
        fprintf(stderr, "Pending recv for socket %d, ignoring subsequent recv\n", fd);
        return -1;
    }

    // lower recv size to fit response APDU payload size
    int max_len = 255 - (MSG_HEADER_LEN + sizeof(int) + sizeof(int)); // 245 bytes
    int actual_len = len > max_len ? max_len : len;

    // schedule worker
    si->pending_recv = msg_id;
    si->pending_recv_data_len = actual_len;
    si->pending_recv_data = malloc(actual_len * sizeof(uint8_t));
    pthread_create(&si->thread_recv, NULL, recv_socket, si);
    pthread_detach(si->thread_recv);
    return 0;
}

void handle_pending_recv(socket_info *si, uint8_t *command_apdu, size_t *command_apdu_len) {
    assert(si->pending_recv != MSG_ID_ILLEGAL);
    assert(si->pending_recv_res != NULL);

    int res = *(si->pending_recv_res);
    if (res > 0) {
        make_socket_recv_response(command_apdu, command_apdu_len, si->pending_recv, res, si->pending_recv_data);
    } else {
        make_socket_recv_response(command_apdu, command_apdu_len, si->pending_recv, res, NULL);
    }

    // clean up
    free(si->pending_recv_res);
    si->pending_recv_res = NULL;

    si->pending_recv_data_len = 0;
    free(si->pending_recv_data);
    si->pending_recv_data = NULL;
    si->pending_recv = MSG_ID_ILLEGAL;
}

void *send_socket(void *arg) {
    socket_info *si = (socket_info *) arg;
    int fd = si->fd;
    int res = send(fd, si->pending_send_data, si->pending_send_data_len, 0);
    printf("[send_worker] socket.send(%d, len=%d) = %d\n", fd, si->pending_send_data_len, res);

    // clean up
    free(si->pending_send_data);
    si->pending_send_data = NULL;
    si->pending_send_data_len = 0;

    // set result
    si->pending_send_res = malloc(sizeof(int));
    *(si->pending_send_res) = res;

    // update stats
    if (res > 0) {
        si->total_bytes_sent += res;
    }

    return NULL;
}

int handle_socket_send_message(uint8_t *message, size_t message_len) {
    // parse message
    int msg_id;
    int fd;
    int len = 0;
    unsigned char *data = NULL;
    if (parse_socket_send_message(message, message_len, &msg_id, &fd, &len, &data) != 0) {
        fprintf(stderr, "Failed to parse SOCKET_SEND message\n");
        return -1;
    }
    assert(len > 0 && data != NULL);

    // find corresponding socket
    struct socket_info *si = find_socket_info(fd);
    if (si == NULL) {
        fprintf(stderr, "Unknown socket fd: %d\n", fd);
        return -1;
    }

    if (si->pending_send != MSG_ID_ILLEGAL) {
        fprintf(stderr, "Pending send for socket fd %d, ignoring subsequent send\n", fd);
        return -1;
    }

    // schedule worker
    si->pending_send = msg_id;
    si->pending_send_data_len = len;
    si->pending_send_data = malloc(len);
    memcpy(si->pending_send_data, data, si->pending_send_data_len);
    pthread_create(&si->thread_send, NULL, send_socket, si);
    pthread_detach(si->thread_send);
    return 0;
}

void handle_pending_send(socket_info *si, uint8_t *command_apdu, size_t *command_apdu_len) {
    assert(si->pending_send != MSG_ID_ILLEGAL);
    assert(si->pending_send_res != NULL);

    int res = *(si->pending_send_res);
    make_socket_send_response(command_apdu, command_apdu_len, si->pending_send, res);

    // clean up
    free(si->pending_send_res);
    si->pending_send_res = NULL;
    si->pending_send = MSG_ID_ILLEGAL;
}

int handle_socket_close_message(uint8_t *message, size_t message_len, uint8_t *command_apdu, size_t *command_apdu_len) {
    int msg_id;
    int fd;
    if (parse_socket_close_message(message, message_len, &msg_id, &fd) != 0) {
        fprintf(stderr, "Failed to parse SOCKET_CLOSE message\n");
        return -1;
    }

    // find corresponding socket
    struct socket_info *si = find_socket_info(fd);
    if (si == NULL) {
        fprintf(stderr, "Unknown socket fd: %d\n", fd);
        return -1;
    }

    printf("Closing socket: %d\n", fd);
    int res = close(fd);
    free_socket_info(fd);
    make_socket_close_response(command_apdu, command_apdu_len, msg_id, res);
    return 0;
}

void process_pending_responses_or_keep_alive(uint8_t *command_apdu, size_t *command_apdu_len) {
    socket_info *si = find_pending_send_socket_info();
    if (si != NULL) {
        handle_pending_send(si, command_apdu, command_apdu_len);
        return;
    }

    si = find_pending_recv_socket_info();
    if (si != NULL) {
        handle_pending_recv(si, command_apdu, command_apdu_len);
        return;
    }

    // idle keep-alive
    make_nfc_link_keep_alive_message(command_apdu, command_apdu_len);
}

void cleanup_sockets() {
    socket_info *si = find_active_socket_info();
    while (si != NULL) {
        free_socket_info(si->fd);
        si = find_active_socket_info();
    }
}

int main(int argc, const char *argv[]) {

    init_socket_infos();

    // initialize NFC reader
    const char *lib_version = nfc_version();
    printf("Using libnfc version: %s\n", lib_version);
    nfc_device *pnd = NULL;
    nfc_context *context = NULL;

    nfc_init(&context);
    if (!context) {
        fprintf(stderr, "Unable to initialize nfc (nfc_init failed)\n");
        exit(EXIT_FAILURE);
    }

    const char *conn_string = argc > 1 ? argv[1] : DEFAULT_CONN_STRING;
    printf("Using conn string: %s\n", conn_string);
    pnd = nfc_open(context, conn_string);
    if (!pnd) {
        fprintf(stderr, "Unable to open device: %s\n", conn_string);
        exit(EXIT_FAILURE);
    }

    if (nfc_initiator_init(pnd) < 0) {
        fprintf(stderr, "nfc_initiator_init failed: %s\n", nfc_strerror(pnd));
        exit(EXIT_FAILURE);
    }

    printf("NFC device opened: %s\n", nfc_device_get_name(pnd));

    // Poll for a ISO14443A (MIFARE) tag
    const nfc_modulation mod_mifare = {
            .nmt = NMT_ISO14443A,
            .nbr = NBR_106,
    };
    nfc_target target;
    while (true) {
        printf("Waiting for device...\n");
        if (nfc_initiator_select_passive_target(pnd, mod_mifare, NULL, 0, &target) > 0) {
            printf("Device selected\n");
            uint8_t command_apdu[512];
            uint8_t response_apdu[512];
            size_t command_apdu_len;
            size_t response_apdu_len;

            // send selection application APDU
            command_apdu_len = 11;
            response_apdu_len = sizeof(response_apdu);
            memcpy(command_apdu, "\x00\xA4\x04\x00\x06\xF0\xAB\xCD\xFF\x00\x00", command_apdu_len);
            if (card_transmit(pnd, command_apdu, command_apdu_len, response_apdu, &response_apdu_len) < 0) {
                fprintf(stderr, "SELECT AID APDU transmission failed: %s\n", nfc_strerror(pnd));
                goto cleanup;
            }
            if (response_apdu_len < 2 || response_apdu[response_apdu_len - 2] != 0x90 ||
                response_apdu[response_apdu_len - 1] != 0x00) {
                fprintf(stderr, "Malformed SELECT AID APDU response\n");
                goto cleanup;
            }

            // application selected, now start processing messages
            make_nfc_link_keep_alive_message(command_apdu, &command_apdu_len);
            while (true) {

                response_apdu_len = sizeof(response_apdu);
                if (card_transmit(pnd, command_apdu, command_apdu_len, response_apdu, &response_apdu_len) < 0) {
                    fprintf(stderr, "Command transmission failed: %s\n", nfc_strerror(pnd));
                    goto cleanup;
                }

                // every message has a type and sub-type
                if (response_apdu_len < MSG_HEADER_LEN) {
                    fprintf(stderr, "Malformed message from smartphone\n");
                    goto cleanup;
                }

                // parse the phone response message
                int msg_type;
                int msg_subtype;
                unsigned char *msg_payload;
                size_t msg_payload_len;
                if (get_message_type(response_apdu, response_apdu_len, &msg_type, &msg_subtype, &msg_payload,
                                     &msg_payload_len) != 0) {
                    fprintf(stderr, "Malformed message from smartphone\n");
                    goto cleanup;
                }

                switch (msg_type) {
                    case MSG_NFC:
                        switch (msg_subtype) {
                            case MSG_NFC_LINK_KEEP_ALIVE:
                                process_pending_responses_or_keep_alive(command_apdu, &command_apdu_len);
                                break;
                            case MSG_NFC_LINK_TERMINATE:
                                goto cleanup;
                            default:
                                fprintf(stderr, "Unknown NFC message type: %d\n", msg_subtype);
                                goto cleanup;
                        }
                        break;
                    case MSG_SOCKET:
                        switch (msg_subtype) {
                            case MSG_SOCKET_CONNECT:
                                handle_socket_connect_message(msg_payload, msg_payload_len, command_apdu,
                                                              &command_apdu_len);
                                break;
                            case MSG_SOCKET_RECV:
                                process_pending_responses_or_keep_alive(command_apdu, &command_apdu_len);
                                handle_socket_recv_message(msg_payload, msg_payload_len);
                                break;
                            case MSG_SOCKET_SEND:
                                process_pending_responses_or_keep_alive(command_apdu, &command_apdu_len);
                                handle_socket_send_message(msg_payload, msg_payload_len);
                                break;
                            case MSG_SOCKET_CLOSE:
                                handle_socket_close_message(msg_payload, msg_payload_len, command_apdu,
                                                            &command_apdu_len);
                                break;
                            default:
                                fprintf(stderr, "Unknown socket message type: %d\n", msg_subtype);
                                goto cleanup;
                        }
                        break;
                    default:
                        fprintf(stderr, "Unknown message type: %d\n", msg_type);
                        goto cleanup;
                }
            }
        }

        cleanup:
        printf("Transaction ended, cleaning up...\n");
        nfc_initiator_deselect_target(pnd);
        cleanup_sockets();

    }

    // clean up
    nfc_close(pnd);
    nfc_exit(context);

    exit(EXIT_SUCCESS);
}




