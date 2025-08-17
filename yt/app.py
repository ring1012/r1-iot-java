from flask import Flask, request, jsonify, Response, stream_with_context
import yt_dlp
import time
import requests
import subprocess
import os

app = Flask(__name__)

# 简单内存缓存 {vId: {"url": xxx, "ts": 时间戳}}
cache = {}
CACHE_TTL = 60 * 30  # 缓存有效期 30 分钟
cookies = {}  # 全局 cookie 缓存

def load_cookies(cookie_file="/root/youtube.txt"):
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

def fetch_youtube_url(vId: str):
    """
    获取或更新指定视频ID的音频URL
    """
    now = time.time()
    if vId in cache:
        entry = cache[vId]
        if now - entry["ts"] < CACHE_TTL:
            return entry["url"], True
        else:
            del cache[vId]  # 过期删除

    ydl_opts = {
        'format': '140',  # Audio only (m4a)
        'cookiefile': '/root/youtube.txt',
        'force_ipv4': True,
        'quiet': True,
        'no_warnings': True,
        'extract_flat': False,
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
            cache[vId] = {"url": url, "ts": now}
            return url, False
        else:
            raise ValueError("URL not found in response")

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

@app.route('/youtube/audio/play/<vId>.m4a', methods=['GET'])
def play_audio(vId):
    """
    获取音频URL并代理返回m4a内容
    """
    try:
        url, _ = fetch_youtube_url(vId)
        headers = {'User-Agent': 'Mozilla/5.0'}

        r = requests.get(url, stream=True, headers=headers, cookies=cookies, timeout=60)
        print(f"[INFO] 请求YouTube音频，状态码: {r.status_code}")

        if r.status_code != 200:
            return jsonify({'error': f'YouTube返回状态码 {r.status_code}'}), 502

        def generate():
            for chunk in r.iter_content(chunk_size=8192):
                if chunk:
                    yield chunk

        resp = Response(stream_with_context(generate()), content_type="audio/mp4")
        resp.headers['Access-Control-Allow-Origin'] = '*'
        return resp
    except Exception as e:
        return jsonify({'error': str(e)}), 500

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
