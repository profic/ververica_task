import socket


def doIt():
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect(('127.0.0.1', 10042))
    i = 0
    while True:
        sock.send('GET 1\n'.encode())
        res = sock.recv(512)
        i += 1
        if i % 10000 == 0:
            print(i)
            print(res)


doIt()
