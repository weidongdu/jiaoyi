#encoding=utf-8
from __future__ import unicode_literals
import json
from http.server import BaseHTTPRequestHandler, HTTPServer


import jieba
import jieba.posseg
import jieba.analyse

# 创建一个请求处理类，继承BaseHTTPRequestHandler
class RequestHandler(BaseHTTPRequestHandler):

    # 处理POST请求的方法
    def do_POST(self):
        # 获取POST请求的内容长度
        content_length = int(self.headers['Content-Length'])

        # 读取POST请求的内容
        post_data = self.rfile.read(content_length)

        # 将POST请求的内容解析为JSON格式
        json_data = json.loads(post_data.decode('utf-8'))

        text = json_data['text'];
        seg_list = jieba.cut_for_search(text)  # 搜索引擎模式
        # print(", ".join(seg_list))

        keywords = list(seg_list);
        # 构造响应数据
        response_data = {'text': text , 'keywords': keywords}

        # 将响应数据转换为JSON格式
        response_json = json.dumps(response_data)

        # 发送响应
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        self.wfile.write(response_json.encode('utf-8'))

# 创建一个HTTP服务器，监听端口为8080，处理请求的类为RequestHandler
httpd = HTTPServer(('localhost', 20001), RequestHandler)

# 开始监听HTTP请求
httpd.serve_forever()
