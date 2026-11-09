# English store media

All promotional copy is English. All device screens are actual captures from this app running on an isolated Android emulator. Geometric frames and text are composed around those captures; no fabricated app screens or third-party stock art are used.

The upload ZIP contains the finished artwork, listing, video, and privacy page. Raw captures and regeneration scripts are available in the project repository.

| Asset | Location | Format and dimensions |
| --- | --- | --- |
| App icon | `artwork/app-icon-512.png` | 32-bit RGBA PNG, 512×512, square corners |
| Feature graphic | `artwork/feature-graphic-1024x500.png` | RGB PNG, 1024×500 |
| Phone | `phone/` | 5 RGB PNGs, 1080×1920, 9:16 |
| 7-inch tablet | `tablet-7/` | 4 RGB PNGs, 1920×1080, 16:9 |
| 10-inch tablet | `tablet-10/` | 4 RGB PNGs, 2560×1440, 16:9 |
| Desktop / wide Android window | `desktop/` | 4 RGB PNGs, 2560×1440, 16:9 |
| Promotion video | `video/adb-local-shell-pro-promo.mp4` | H.264 MP4, 1920×1080, 24 fps, 30 seconds, silent |
| Original captures | `raw/` | Native app UI at phone, tablet and desktop viewport sizes |
| Listing text | `listing/` | Plain UTF-8 text |

Image file sizes and listing character counts are validated by `tools/create_store_assets.py` and listed in `manifest.json`. Phone images show the full device with feature captions. Tablet/desktop sets show the app's adaptive interface in complete landscape frames. The 7-inch and 10-inch artwork shares tablet UI at the relevant output sizes, rather than suggesting hardware-specific features.

The desktop images represent Android desktop/windowed usage. This project does not include a Windows, macOS or Linux desktop app. The 7-inch source viewport is 960×600 dp; the 10-inch source viewport is 1280×800 dp. Both are captured at 1440×900 physical pixels with different emulator densities. Phone captures are 720×1600; wide Android captures are 1920×1080. Store compositions use the output dimensions listed above.

## Regenerate

1. Install `tools/requirements.txt` into your Python environment.
2. Build debug and instrumentation APKs. Install them on an isolated development emulator using `tools/device_qa.py install`.
3. Pair with the emulator's Wireless debugging service. Capture each viewport with `device_qa.py capture --prefix phone`, `--prefix tablet7`, `--prefix tablet`, and `--prefix desktop`, supplying `--port` for the active local connection port. The capture test runs real read-only diagnostic commands and then records the UI.
4. Run `python tools/create_store_assets.py --video` to render artwork, the contact sheet, validation manifest and video. Omit `--video` to regenerate images only.
5. Inspect the contact sheet, full images and several video frames before upload. Raw captures may contain device model and diagnostic output; review them before generating a public store package.

The video intentionally contains no audio or music. This avoids third-party audio licensing and communicates its features through readable English captions. You upload it to YouTube, set public/unlisted visibility with no ads or age restriction, and paste that URL into Play Console.

Typography: Space Grotesk (SIL OFL 1.1). Source font and license are included. Icon and device frames are original vector/geometric designs.
