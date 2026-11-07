#!/usr/bin/env python3
"""Snapshot the names and descriptions of the Vanilla Tweaks packs for the site. Icons and previews stay on their site."""
import html, json, re, urllib.request
from pathlib import Path

VERSION = '26.2'
URL = f'https://vanillatweaks.net/assets/resources/json/{VERSION}/rpcategories.json'
OUT = Path(__file__).resolve().parent.parent / 'public/assets/screen/data.json'


def plain(text):
    return re.sub(r'\s+', ' ', html.unescape(re.sub(r'<[^>]+>', '', re.sub(r'<br\s*/?>', ' ', text or '')))).strip()


def rich(text):
    """The description as [text, colour] runs, as the mod colours it from the description's spans."""
    runs, stack = [], [None]
    for tag, close, attrs, chunk in re.findall(r'(<(/?)(?:span|font)([^>]*)>)|([^<]+|<[^>]*>)', text or ''):
        if tag:
            if close:
                if len(stack) > 1:
                    stack.pop()
            else:
                m = re.search(r'color:\s*(#[0-9a-fA-F]{6})', attrs)
                stack.append(m.group(1).lower() if m else stack[-1])
            continue
        piece = plain(chunk) if not re.fullmatch(r'<br\s*/?>', chunk) else ' '
        if not chunk.startswith('<') and chunk[:1].isspace():
            piece = ' ' + piece
        if not chunk.startswith('<') and chunk[-1:].isspace() and piece.strip():
            piece = piece + ' '
        if piece:
            runs.append([piece, stack[-1]])
    joined = re.sub(r' +', ' ', ''.join(r[0] for r in runs)).strip()
    if not any(r[1] for r in runs) or joined != plain(text):
        return None
    # Collapse the spaces the same way plain() does, run by run
    out, last = [], ' '
    for piece, color in runs:
        piece = re.sub(r' +', ' ', piece)
        if last == ' ':
            piece = piece.lstrip(' ')
        if piece:
            out.append([piece, color] if color else [piece])
            last = piece[-1]
    out[-1][0] = out[-1][0].rstrip()
    return out


def pack(p):
    out = {'id': p['name'], 'name': p['display'], 'text': plain(p.get('description'))}
    runs = rich(p.get('description'))
    if runs:
        out['rich'] = runs
    if p.get('incompatible'):
        out['not'] = p['incompatible']
    if p.get('previewExtension'):
        out['ext'] = p['previewExtension']
    return out


def category(c):
    out = {'name': c['category'], 'packs': [pack(p) for p in c.get('packs', []) if not p.get('experiment')]}
    if c.get('warning'):
        out['warning'] = {'text': plain(c['warning']['text']), 'color': c['warning']['color']}
    if c.get('categories'):
        out['sub'] = [category(s) for s in c['categories']]
    if c.get('hardIncompatible') or c.get('hard_incompatible'):
        out['exclusive'] = True
    return out


request = urllib.request.Request(URL, headers={'User-Agent': 'vtdownloader-wiki'})
data = json.load(urllib.request.urlopen(request, timeout=30))
OUT.write_text(json.dumps({'version': VERSION, 'categories': [category(c) for c in data['categories']]},
                          ensure_ascii=False, separators=(',', ':')))
print(OUT, OUT.stat().st_size)
