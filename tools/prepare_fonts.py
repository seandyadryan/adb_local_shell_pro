from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
import shutil

root = Path(__file__).resolve().parents[1]
source = root / 'tools/fonts/SpaceGrotesk-Variable.ttf'
out = root / 'app/src/main/res/font'
out.mkdir(parents=True, exist_ok=True)
for name, weight in [('medium', 500), ('bold', 700)]:
    font = instantiateVariableFont(TTFont(source), {'wght': weight}, inplace=False)
    font.save(out / f'space_{name}.ttf')
shutil.copy(root / 'third-party-licenses/SpaceGrotesk-OFL.txt', root / 'app/src/main/assets/SpaceGrotesk-OFL.txt')
print('Prepared Space Grotesk font assets.')
