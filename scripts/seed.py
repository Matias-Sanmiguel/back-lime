#!/usr/bin/env python3
"""Carga usuarios y avisos de demostración en una API Lime en ejecución.

Uso: python3 scripts/seed.py [--api http://localhost:8080]

Requiere solo la biblioteca estándar. Si un usuario ya tiene avisos, se omite.
"""
import argparse
import json
import mimetypes
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

DATA = Path(__file__).with_name('seed-data.json')


class Api:
    def __init__(self, base):
        self.base = base.rstrip('/')
        self.token = None

    def request(self, method, path, body=None, headers=None):
        headers = dict(headers or {})
        if self.token:
            headers['Authorization'] = f'Bearer {self.token}'
        if isinstance(body, (dict, list)):
            body = json.dumps(body).encode()
            headers['Content-Type'] = 'application/json'
        req = urllib.request.Request(self.base + path, data=body, method=method, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=60) as res:
                raw = res.read()
                return res.status, json.loads(raw) if raw else None
        except urllib.error.HTTPError as err:
            raw = err.read()
            try:
                return err.code, json.loads(raw)
            except ValueError:
                return err.code, raw.decode(errors='replace')

    def upload(self, path, filename, content, content_type):
        boundary = uuid.uuid4().hex
        body = b''.join([
            f'--{boundary}\r\n'.encode(),
            f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'.encode(),
            f'Content-Type: {content_type}\r\n\r\n'.encode(),
            content,
            f'\r\n--{boundary}--\r\n'.encode(),
        ])
        return self.request('POST', path, body, {'Content-Type': f'multipart/form-data; boundary={boundary}'})


def download(url):
    req = urllib.request.Request(url, headers={'User-Agent': 'lime-seed'})
    with urllib.request.urlopen(req, timeout=60) as res:
        content_type = res.headers.get_content_type()
        return res.read(), content_type


def authenticate(api, user):
    payload = {k: user[k] for k in ('email', 'password', 'name', 'role', 'agencyName', 'birthDate', 'sex')}
    status, body = api.request('POST', '/api/v1/auth/register', payload)
    if status >= 400:
        status, body = api.request('POST', '/api/v1/auth/login',
                                   {'email': user['email'], 'password': user['password']})
    if status >= 400:
        sys.exit(f"No se pudo autenticar {user['email']}: {status} {body}")
    api.token = body['token']


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('--api', default='http://localhost:8080')
    args = parser.parse_args()

    data = json.loads(DATA.read_text())
    api = Api(args.api)
    cache = {}

    for user in data['users']:
        authenticate(api, user)
        _, mine = api.request('GET', '/api/v1/me/properties?size=1')
        if mine and mine.get('totalElements', 0) > 0:
            print(f"{user['email']}: ya tiene avisos, se omite")
            continue

        owned = [p for p in data['properties'] if p['owner'] == user['key']]
        for prop in owned:
            status, created = api.request('POST', '/api/v1/properties', prop['request'])
            if status >= 400:
                sys.exit(f"Error creando '{prop['request']['title']}': {status} {created}")
            pid = created['id']

            for n, url in enumerate(prop['images'], 1):
                if url not in cache:
                    cache[url] = download(url)
                content, content_type = cache[url]
                ext = mimetypes.guess_extension(content_type) or '.jpg'
                status, body = api.upload(f'/api/v1/properties/{pid}/images', f'foto-{n}{ext}', content, content_type)
                if status >= 400:
                    sys.exit(f'Error subiendo foto al aviso {pid}: {status} {body}')

            if prop['status'] in ('PUBLISHED', 'PAUSED'):
                api.request('POST', f'/api/v1/properties/{pid}/publish')
            if prop['status'] == 'PAUSED':
                api.request('POST', f'/api/v1/properties/{pid}/pause')
            print(f"  #{pid} {prop['status']:<9} {prop['request']['title']}")
        print(f"{user['email']}: {len(owned)} avisos cargados")


if __name__ == '__main__':
    main()
