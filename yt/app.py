from flask import Flask, request, jsonify, Response, stream_with_context
import yt_dlp
import time
import requests
import subprocess
import os
from urllib.parse import quote_plus, parse_qsl, urlencode
from cachetools import TTLCache
from threading import Lock
from functools import lru_cache
import hashlib


app = Flask(__name__)


CACHE_TTL = 3600
cache = TTLCache(maxsize=1000, ttl=CACHE_TTL)
POST_CACHE = TTLCache(maxsize=1000, ttl=1)

deno_path='home/container/deno'

@lru_cache(maxsize=None)
def _lock_for(vId: str) -> Lock:
    # lru_cache 自带全局锁，线程安全地返回“每个 key 的同一把锁”
    return Lock()

cookies = {}  # 全局 cookie 缓存

def load_cookies(cookie_file="/home/container/youtube.txt"):
    """
    读取 youtube.txt (Netscape 格式) 的 cookie 并缓存
    """
    global cookies
    if not os.path.exists(cookie_file):
        print(f"[WARN] cookie 文件不存在: {cookie_file}")
        return

    try:
        with open(cookie_file, "r", encoding="utf-8") as f:
            for line in f:
                if not line.strip() or line.startswith("#"):
                    continue
                parts = line.strip().split("\t")
                if len(parts) >= 7:
                    name, value = parts[-2], parts[-1]
                    cookies[name] = value
        print(f"[INFO] 成功加载 {len(cookies)} 个 cookie")
    except Exception as e:
        print(f"[ERROR] 读取 cookie 文件失败: {e}")

def _load_youtube_url(vId: str):
    ydl_opts = {
        'format': '140',  # Audio only (m4a)
        'cookiefile': '/home/container/youtube.txt',
        'force_ipv4': True,
        'quiet': True,
        'no_warnings': True,
        'extract_flat': False,
        'js_runtimes': {'deno': {'executable': '/home/container/deno'}}
    }

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(f'https://www.youtube.com/watch?v={vId}', download=False)

        url = None
        # 优先直接取 url
        if 'url' in info:
            url = info['url']
        # 否则在 formats 里找 m4a
        elif 'formats' in info:
            for f in info['formats']:
                if f.get('format_id') == '140':
                    url = f.get('url')
                    break

        if url:
            print(f"缓存: {vId}")
            return url
        else:
            raise ValueError("URL not found in response")
        
def fetch_youtube_url(vId: str):
   
    # 1) 无锁快读
    url = cache.get(vId)
    if url is not None:
        print(f"使用缓存: {vId}")
        return url, True

    # 2) 单航班：同一 vId 共享同一把锁
    lock = _lock_for(vId)
    with lock:
        # 3) 双检
        url = cache.get(vId)
        if url is not None:
            print(f"使用缓存: {vId}")
            return url, True

        # 4) 真抓一次并写入
        url = _load_youtube_url(vId)
        cache[vId] = url
        return url, False
    

@app.route('/youtube/get_youtube_url', methods=['GET'])
def get_youtube_url():
    """
    获取YouTube音频URL的接口

    参数:
    vId - YouTube视频ID

    返回:
    JSON格式的音频URL或错误信息
    """
    vId = request.args.get('vId')
    if not vId:
        return jsonify({'error': 'Missing vId parameter'}), 400

    try:
        url, cached = fetch_youtube_url(vId)
        return jsonify({'url': url, 'cached': cached}), 200
    except yt_dlp.utils.DownloadError as e:
        return jsonify({'error': f'YouTube DL error: {str(e)}'}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/youtube/audio/play/<vId>.m4a', methods=['GET', 'HEAD'])
def play_audio(vId):
    try:
        url, _ = fetch_youtube_url(vId)  # 你的实现，返回直链

        # 基本 headers，透传客户端的 Range（如果有）
        upstream_headers = {'User-Agent': 'Mozilla/5.0'}
        client_range = request.headers.get('Range')
        print(client_range)
        if client_range:
            upstream_headers['Range'] = client_range

        # 向上游请求（stream=True 保证按需读取）
        r = requests.get(url, stream=True, headers=upstream_headers, cookies=cookies, timeout=60)

        if r.status_code not in (200, 206):
            return jsonify({'error': f'YouTube 返回状态码 {r.status_code}'}), 502

        # 如果是 HEAD 请求，只返回头部
        if request.method == 'HEAD':
            resp = Response(status=r.status_code)
            # 复制上游重要头
            for h in ('Content-Type', 'Content-Length', 'Content-Range', 'Accept-Ranges', 'ETag', 'Last-Modified'):
                if h in r.headers:
                    resp.headers[h] = r.headers[h]
            resp.headers['Access-Control-Allow-Origin'] = '*'
            return resp

        # 流式把上游内容推给客户端
        def generate():
            try:
                for chunk in r.iter_content(chunk_size=8192):
                    if chunk:
                        yield chunk
            finally:
                r.close()

        resp = Response(stream_with_context(generate()), status=r.status_code)
        # 复制必要的头（不要 blindly 复制 Transfer-Encoding）
        if 'Content-Type' in r.headers:
            resp.headers['Content-Type'] = r.headers['Content-Type']
        for h in ('Content-Length', 'Content-Range', 'Accept-Ranges', 'ETag', 'Last-Modified'):
            if h in r.headers:
                resp.headers[h] = r.headers[h]

        resp.headers['Access-Control-Allow-Origin'] = '*'
        return resp

    except Exception as e:
        return jsonify({'error': str(e)}), 500

    
# gemini
TARGET_BASE = "https://generativelanguage.googleapis.com/v1beta"

# cache by query
CACHE_SVC = "https://yt.hutang.cloudns.be/gemini/v1beta"


def call_google_api(path, query, body, headers):
    target_url = f"{TARGET_BASE}/{path}"
    if query:
        target_url += "?" + urlencode(query)
    resp = requests.post(target_url, headers=headers, json=body, timeout=60)
    return resp

@app.route('/v1beta', defaults={'path': ''}, methods=['POST'])
@app.route('/v1beta/<path:path>', methods=['POST'])
def proxy_v1beta(path):
    incoming_body = request.json or {}
    incoming_query = dict(request.args)

    userInput = None
    for c in incoming_body.get('contents', []):
        if c.get('role') == 'user' and c.get('parts'):
            text = c['parts'][0].get('text')
            if text:
                userInput = text
                break

    google_headers = {
        k: v for k, v in request.headers.items() if k.lower() not in ['host', 'content-length']
    }

    if userInput and "现在" in userInput:
        print("命中关键词：现在 → 不缓存，直接请求 Google API")
        resp = call_google_api(path, incoming_query, incoming_body, google_headers)
        excluded_headers = ['content-encoding', 'transfer-encoding', 'connection']
        response_headers = [(name, value) for name, value in resp.raw.headers.items()
                            if name.lower() not in excluded_headers]

        return Response(resp.content, resp.status_code, response_headers)

    key = hashlib.md5((userInput or "").encode()).hexdigest()

    POST_CACHE[key] = {
        "body": incoming_body,
        "query": incoming_query,
        "headers": google_headers
    }

    gemini_url = f"{CACHE_SVC}/{path}?userInput={key}"
    resp = requests.get(gemini_url, timeout=60)
    excluded_headers = ['content-encoding', 'transfer-encoding', 'connection']
    response_headers = [(name, value) for name, value in resp.raw.headers.items()
                        if name.lower() not in excluded_headers]

    return Response(resp.content, resp.status_code, response_headers)

@app.route('/gemini/v1beta', defaults={'path': ''}, methods=['GET'])
@app.route('/gemini/v1beta/<path:path>', methods=['GET'])
def proxy_gemini(path):
    key = request.args.get("userInput")
    data = POST_CACHE.get(key)
    if not data:
        return {"error": "token expired"}, 410

    original_body = data["body"]
    original_query = data["query"]
    cached_headers = data.get("headers", {})

    resp = call_google_api(path, original_query, original_body, cached_headers)
    excluded_headers = ['content-encoding', 'transfer-encoding', 'connection']
    response_headers = [(name, value) for name, value in resp.raw.headers.items()
                        if name.lower() not in excluded_headers]

    return Response(resp.content, resp.status_code, response_headers)

# audio youtube
YOUTUBE_BASE = "https://www.youtube.com/results"

@app.route('/youtube/search/audio', methods=['GET'])
def youtube_search_proxy():
    keyword = request.args.get('keyword', '')
    suffix = request.args.get('suffix', '')  # 如果你想加后缀
    search_query = f"{keyword} {suffix}".strip()

    # 构造 URL
    url = f"{YOUTUBE_BASE}?search_query={quote_plus(search_query)}"

    # 构造 headers
    headers = {
        "accept-language": "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
        # 你可以加更多 header，例如 User-Agent
        "User-Agent": request.headers.get("User-Agent", "Mozilla/5.0")
    }

    # 发起请求
    resp = requests.get(url, headers=headers, stream=True)

    # 过滤部分 header 避免冲突
    excluded_headers = ['content-encoding', 'transfer-encoding', 'connection']
    response_headers = [(k, v) for k, v in resp.headers.items() if k.lower() not in excluded_headers]

    # 返回响应
    return Response(resp.content, status=resp.status_code, headers=response_headers)

# music youtube
MUSIC_SEARCH_URL = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"



# 原始请求 payload 模板
SAMPLE_PAYLOAD = """{"context":{"client":{"hl":"en","gl":"SG","remoteHost":"202.6.40.167","deviceMake":"Apple","deviceModel":"","visitorData":"Cgs4QXVOYVV4OUp0TSj28rfABjIKCgJDThIEGgAgSA%3D%3D","userAgent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36,gzip(gfe)","clientName":"WEB_REMIX","clientVersion":"1.20250423.01.00","osName":"Macintosh","osVersion":"10_15_7","originalUrl":"https://music.youtube.com/search?q=%E5%91%A8%E6%9D%B0%E4%BC%A6","platform":"DESKTOP","clientFormFactor":"UNKNOWN_FORM_FACTOR"}},"query":"__KEYWORD__","params":"EgWKAQIIAWoSEAMQBBAJEA4QChAFEBEQEBAV"}"""


@app.route("/youtube/search/music", methods=["GET"])
def youtube_music_proxy():
    keyword = request.args.get("keyword", "").strip()
    if not keyword:
        return {"error": "keyword is required"}, 400

    # 替换 SAMPLE_PAYLOAD 中的 query 字段
    payload = SAMPLE_PAYLOAD.replace("__KEYWORD__", keyword)

    headers = {
        "Content-Type": "application/json",
        "User-Agent": request.headers.get("User-Agent", "Mozilla/5.0")
    }

    resp = requests.post(MUSIC_SEARCH_URL, headers=headers, data=payload)

    # 直接把响应透传给客户端
    excluded_headers = ['content-encoding', 'transfer-encoding', 'connection']
    response_headers = [(k, v) for k, v in resp.headers.items() if k.lower() not in excluded_headers]

    return Response(resp.content, status=resp.status_code, headers=response_headers)



# app.config['UPLOAD_FOLDER'] = '/var/data'
#
# # 确保上传目录存在
# os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
#
# @app.route('/upload', methods=['POST'])
# def upload_file():
#     # 检查 Authorization 头部
#     auth_header = request.headers.get('Authorization')
#     if not auth_header or auth_header != 'Bearer dGFuZzpodWFu':
#         return jsonify({'error': 'Unauthorized'}), 401
#
#     if 'file' not in request.files:
#         return jsonify({'error': 'No file part'}), 400
#
#     file = request.files['file']
#     if file.filename == '':
#         return jsonify({'error': 'No selected file'}), 400
#
#     # 定义保存文件的路径
#     file_path = os.path.join(app.config['UPLOAD_FOLDER'], file.filename)
#
#     # 保存文件（会覆盖现有文件）
#     file.save(file_path)
#
#     return jsonify({'message': 'File uploaded and overwritten successfully'}), 200
#

if __name__ == '__main__':
    app.run(host='::', port=8080)
