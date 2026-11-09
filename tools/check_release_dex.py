"""Reject release packages that contain the unsupported Android SDK access paths."""
from pathlib import Path
import re
import sys
import zipfile

BLOCKED = (
    b'Lorg/lsposed/hiddenapibypass/',
    b'Lcom/flyfishxu/kadb/',
    b'setHiddenApiExemptions',
    b'dalvik.system.VMRuntime',
    b'Lcom/android/org/conscrypt/',
    b'com.android.org.conscrypt.Conscrypt',
)


def check(path: Path) -> None:
    with zipfile.ZipFile(path) as archive:
        dex = [name for name in archive.namelist() if re.search(r'(?:^|/)classes\d*\.dex$', name)]
        if not dex:
            raise ValueError(f'No DEX files in {path}')
        for name in dex:
            data = archive.read(name)
            for marker in BLOCKED:
                if marker in data:
                    raise ValueError(f'{path.name} / {name} references {marker.decode()}')
        print(f'{path.name}: inspected {len(dex)} DEX files. No blocked SDK references.')


if __name__ == '__main__':
    if len(sys.argv) < 2:
        raise SystemExit('Provide at least one APK or AAB path.')
    for arg in sys.argv[1:]:
        check(Path(arg))
