"""Fetch only the Material Symbols used by the app (Apache 2.0, Google).
Generated native vector resources are committed; normal builds need no network.
"""
from pathlib import Path
import concurrent.futures,urllib.request,xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
names=['terminal','bookmarks','history','cable','tune','close','content_copy','delete_sweep','stop','arrow_forward','bookmark_add','open_in_new','search','add','search_off','delete','check_circle','error','lock','policy','code','mail']
def fetch(name):
    url=f'https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/{name}/materialsymbolsoutlined/{name}_24px.svg'
    svg=ET.fromstring(urllib.request.urlopen(url,timeout=30).read())
    x,y,w,h=map(float,svg.attrib['viewBox'].split())
    paths=[n.attrib['d'] for n in svg.iter() if n.tag.endswith('path') and 'd' in n.attrib]
    assert paths,name
    mirror=' android:autoMirrored="true"' if name in ['arrow_forward','open_in_new'] else ''
    body=f'<!-- Google Material Symbols Outlined, Apache License 2.0. Source: {url} -->\n'
    body+=f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="{w:g}" android:viewportHeight="{h:g}"{mirror}>\n'
    body+=f'    <group android:translateX="{-x:g}" android:translateY="{-y:g}">\n'
    for d in paths: body+=f'        <path android:fillColor="#FFFFFFFF" android:pathData="{d}" />\n'
    body+='    </group>\n</vector>\n'
    (ROOT/f'app/src/main/res/drawable/ic_{name}.xml').write_text(body,encoding='utf-8')
    return name
if __name__=='__main__':
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        for name in pool.map(fetch,names): print(name)
