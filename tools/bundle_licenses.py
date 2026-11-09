from pathlib import Path
import re

root=Path(__file__).resolve().parents[1]
native=root/'pairing-native/src/main/cpp'
native_notices=['Native SPAKE2 source copyright and permission notices\n']
for source in sorted(native.rglob('*')):
    if source.suffix not in ('.c','.cpp','.h'): continue
    for comment in re.findall(r'/\*.*?\*/',source.read_text(encoding='utf-8'),flags=re.S):
        if 'copyright' in comment.lower():
            native_notices.append('\n'+str(source.relative_to(root))+'\n'+comment+'\n')
(root/'third-party-licenses/Native-pairing-notices.txt').write_text(''.join(native_notices),encoding='utf-8')
parts=[(root/'THIRD_PARTY_NOTICES.md').read_text(encoding='utf-8')]
for path in sorted((root/'third-party-licenses').glob('*.txt')):
    parts.append('\n\n'+'='*60+'\n'+path.name+'\n'+'='*60+'\n'+path.read_text(encoding='utf-8'))
(root/'app/src/main/assets/licenses.txt').write_text(''.join(parts),encoding='utf-8')
print('Bundled offline license notices.')
