"""Create the Play upload handoff without raw captures or build credentials."""
from pathlib import Path
import hashlib
import zipfile

ROOT = Path(__file__).resolve().parents[1]
STORE = ROOT / 'store'

if __name__ == '__main__':
    out = ROOT / 'output/release'
    out.mkdir(parents=True, exist_ok=True)
    files = []
    for folder in ['listing', 'phone', 'tablet-7', 'tablet-10', 'desktop']:
        files.extend(sorted((STORE / folder).glob('*')))
    files.extend(STORE / name for name in [
        'artwork/app-icon-512.png', 'artwork/feature-graphic-1024x500.png',
        'video/adb-local-shell-pro-promo.mp4', 'video/poster.jpg',
        'manifest.json', 'README.md', 'PLAY_CONSOLE.md',
    ])
    target = out / 'adb-local-shell-pro-play-store-kit.zip'
    with zipfile.ZipFile(target, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as bundle:
        for path in files:
            if not path.is_file():
                raise FileNotFoundError(path)
            bundle.write(path, 'store/' + path.relative_to(STORE).as_posix())
        bundle.write(ROOT / 'docs/index.html', 'privacy/index.html')
        bundle.write(ROOT / 'QA_REPORT.md', 'QA_REPORT.md')
    digest = hashlib.sha256(target.read_bytes()).hexdigest()
    (out / 'store-kit.sha256').write_text(f'{digest}  {target.name}\n', encoding='utf-8')
    print(f'{target}: {target.stat().st_size:,} bytes, SHA-256 {digest}')
