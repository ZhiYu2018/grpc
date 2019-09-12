#!/bin/python3
# -*- coding: utf-8 -*-

import requests
import threading
import time

class UpdateThread(threading.Thread):
    ''''''
    def __init__(self, app_id):
        ''''''
        threading.Thread.__init__(self)
        self.aid = app_id

    def __del__(self):
        ''''''
        pass

    def run(self):
        ''''''
        url = "http://192.168.3.25:8080/grpc/route"
        data = {"app_id": self.aid, "method":"sayHello", "format":"json", "charset":"utf8","timestamp":"2019-12-12 12:44:55"}
        headers = {"X-Grpc-Method":"com.gexiang.grpc.GrpcGate/Route", "X-Grpc-Ver":"v1.0"}
        failed = 0
        ticks = time.time()
        for i in range(0,4000):
            ''''''
            if (i % 500) == 0:
                ''''''
                time.sleep(3)
            ''''''
            r = requests.post(url, json=data, headers=headers)
            if r.status_code != 200:
                failed = failed + 1
                continue
            ''''''
        ''''''
        perf = ((time.time() - ticks) * 1000)/4000
        print ("App id ", self.aid, ",Perf ", perf, ",Failed ", failed)


if __name__ == '__main__':
    ''''''
    tq = []
    for i in range(0, 8):
        ''''''
        th = UpdateThread("app_%d" % i)
        th.start()
        tq.append(th)
    ''''''
    for t in tq:
        ''''''
        th.join()
    ''''''
