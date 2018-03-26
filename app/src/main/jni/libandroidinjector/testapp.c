/*
 * testapp.c
 *
 *  Created on: 2013-1-17
 *      Author: d
 */

#ifndef TESTAPP_C_
#define TESTAPP_C_
#include <pthread.h>
#include <netinet/in.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <arpa/inet.h>
#include <stdio.h>
#include <unistd.h>
#include <dlfcn.h>
static void connect_to(char *host, int port);
int main(int argc, char* argv[]) {
    printf("pid %d\n", getpid());
    printf("dlopen 0x%08x\n", dlopen);
    printf("dlsym 0x%08x\n", dlsym);

    connect_to("192.168.1.1", 80);
    return 0;
}

static void connect_to(char *ip, int port) {
    while (1) {
        sleep(10);
        struct sockaddr_in client_addr;
        bzero(&client_addr, sizeof(client_addr));
        client_addr.sin_family = AF_INET;
        client_addr.sin_addr.s_addr = htons(INADDR_ANY);
        client_addr.sin_port = htons(0);
        int client_socket = socket(AF_INET, SOCK_STREAM, 0);
        if (client_socket < 0) {
            printf("socket failure\n");
            continue;
        }

        struct sockaddr_in server_addr;
        bzero(&server_addr, sizeof(server_addr));
        server_addr.sin_family = AF_INET;
        if (inet_aton(ip, &server_addr.sin_addr) == 0) {
            printf("address error\n");
            continue;
        }
        server_addr.sin_port = htons(port);
        socklen_t server_addr_length = sizeof(server_addr);
        printf("%p\n", connect);
        if (connect(client_socket, (struct sockaddr*) &server_addr,
                server_addr_length) < 0) {
            printf("connect to %s %d failed\n", ip, port);
            continue;
        }
        printf("connect to %s %d ok\n", ip, port);
        close(client_socket);

    }

}

#endif /* TESTAPP_C_ */
