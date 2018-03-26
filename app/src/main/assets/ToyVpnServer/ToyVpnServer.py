#!/usr/bin/python
# coding:utf-8
"""
__version__ = '1.0'
__date__  = '2013-07-29'
__author__ = "shaozheng.wu@gmail.com"
"""

from __future__ import with_statement
import sys
if sys.version_info < (2, 6):
    import simplejson as json
else:
    import json

import os
import hashlib
import getopt
import fcntl
import time
import struct
import socket, select
import traceback
import signal
import logging

TUNSETIFF = 0x400454ca
IFF_TUN   = 0x0001
IFF_NO_PI = 0x1000

DEBUG = 0
TIMEOUT = 60*10
BUFFER_SIZE = 0x7fff

class Tunnel():
    def create(self):
        try:
            self.tfd = os.open("/dev/net/tun", os.O_RDWR)
        except:
            self.tfd = os.open("/dev/tun", os.O_RDWR)
        ifs = fcntl.ioctl(self.tfd, TUNSETIFF, struct.pack("16sH", "tun%d", IFF_TUN|IFF_NO_PI))
        self.tname = ifs[:16].strip("\x00")

    def close(self):
        os.close(self.tfd)

    def config(self, ip):
        logging.info("Configuring interface %s with ip %s" % (self.tname, ip))
        os.system("ip link set %s up" % (self.tname))
        os.system("ip addr add %s dev %s" % (ip, self.tname))
        os.system("ifconfig %s netmask 255.255.255.0" %self.tname)

    def run(self):
        global PORT
        self.udpfd = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.udpfd.bind(("", PORT))
        self.clients = {}
        self.reaped_key = []
        while True:
            rset = select.select([self.udpfd, self.tfd], [], [], 1)[0]
            if self.tfd in rset:
                if DEBUG: os.write(1, ">")
                data = os.read(self.tfd,BUFFER_SIZE)
                dst = data[16:20]
                if dst in self.clients:
                    self.udpfd.sendto(data, self.clients[dst]['localIPn'])
                else:
                    logging.warn("There's no client %s connected to server" %socket.inet_ntoa(dst))
                # Remove timeout clients
                curTime = time.time()
                for key in self.clients.keys():
                    if curTime - self.clients[key]["aliveTime"] > TIMEOUT:
                        logging.warn("Remove timeout client %s,%s" %self.clients[key]['localIPn'])
                        self.reaped_key.append(key)
                        del self.clients[key]

            if self.udpfd in rset:
                if DEBUG: os.write(1, "<")
                data, src = self.udpfd.recvfrom(BUFFER_SIZE)
                # Simply write the packet to local or forward them to other clients
                if data[0] != chr(0):
                    if data[12:16] in self.clients:
                        os.write(self.tfd, data)
                        self.clients[data[12:16]]["aliveTime"] = time.time()
                        self.clients[data[12:16]]["localIPn"] = src
                    else:
                        logging.warn("There's no client %s connected to server" %socket.inet_ntoa(data[12:16]))
                else:
                    login = False
                    for key in self.clients.keys():
                        if self.clients[key]["localIPn"] == src:
                            login = True
                            break
                    if login:
                       if len(data) == 1:
                           self.udpfd.sendto(chr(0) + "KEEP ALIVE", src)
                       continue
                    if data[1:] == KEY:
                        localIP = parameters['address'].split(',')[0]
                        if self.reaped_key:
                            localIP = socket.inet_ntoa(self.reaped_key.pop())
                        else:
                            IPchr = socket.inet_aton(localIP)
                            if self.clients:
                                IPchr = sorted(self.clients)[-1]
                            IPchr = socket.inet_aton(localIP)
                            IPchr = struct.pack('>I',struct.unpack('>I',IPchr)[0] + 1)
                            self.clients[IPchr] = {"aliveTime": time.time(), "localIPn": src}
                            localIP = socket.inet_ntoa(IPchr)
                            parameters['address'] = localIP+',32'  #it's used for Compatible with android ToyVPN Client
                        localIPn_key =  socket.inet_aton(localIP)
                        self.clients[localIPn_key] = {"aliveTime": time.time(), "localIPn": src}

                        parameters['address'] = localIP + ',32'
                        logging.info("New Client from %s request IP %s" %(src,localIP))
                        logging.info("send parameters == %s" %(chr(0) + " ".join(["%s,%s" % (k[0], parameters[k]) for k in sorted(parameters.keys())])))
                        for i in xrange(3):
                            self.udpfd.sendto(chr(0) + " ".join(["%s,%s" % (k[0], parameters[k]) for k in sorted(parameters.keys())]), src)
                    else:
                        logging.error("Need valid password from %s,%s" %src)
                        self.udpfd.sendto(chr(0) + "WRONG PASSWORD", src)


def usage(status = 0):
    print "Usage: %s [-p port|-k key|-h help|-d debug] or configurate your config.json correctly" % (sys.argv[0])
    sys.exit(status)

def on_exit(no, info):
    raise Exception("TERM signal caught!")

if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG, format='%(asctime)s %(levelname)-8s %(message)s', datefmt='%Y-%m-%d %H:%M:%S', filemode='a+')

    with open('config.json', 'rb') as f:
        parameters = json.load(f)
    PORT = parameters['server_port']
    KEY = parameters['password']
    del parameters['server_port'],parameters['password']
    opts = getopt.getopt(sys.argv[1:],"p:k:hd")
    for opt,optarg in opts[0]:
        if opt == "-h":
            usage()
        elif opt == "-p":
            PORT = int(optarg)
        elif opt == "-k":
            KEY  = optarg
        elif opt == "-d":
            DEBUG = True

    if not PORT or not KEY or len(parameters.keys()) < 4:
        usage(1)

    tun = Tunnel()
    tun.create()
    tun.config(parameters['address'].split(',')[0])

    signal.signal(signal.SIGTERM, on_exit)
    signal.signal(signal.SIGTSTP, on_exit)
    try:
        tun.run()
    except KeyboardInterrupt:
        pass
    except:
        print traceback.format_exc()
    finally:
        tun.close()

