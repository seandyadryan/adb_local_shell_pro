"""Run repeatable QA only against the explicitly selected Android emulator.

Never defaults to a physical device. Outputs and raw captures are project-local.
"""
from pathlib import Path
import argparse
import os
import subprocess
import time
import sys
import re
import xml.etree.ElementTree as ET
sys.stdout.reconfigure(encoding='utf-8')

ROOT=Path(__file__).resolve().parents[1]
SDK=Path(os.environ.get('ANDROID_HOME', Path(os.environ.get('LOCALAPPDATA',''))/'Android/Sdk'))
ADB=SDK/'platform-tools'/('adb.exe' if os.name=='nt' else 'adb')
PKG='com.deploydulupulangnanti.adblocalshellpro'

def adb(*args, binary=False, timeout=120):
    command=[str(ADB),'-s',SERIAL,*map(str,args)]
    result=subprocess.run(command,capture_output=True,timeout=timeout)
    if result.returncode:
        raise RuntimeError((result.stdout+result.stderr).decode('utf-8',errors='replace'))
    return result.stdout if binary else result.stdout.decode('utf-8',errors='replace')

def instrument(cls, **kwargs):
    classes=cls if isinstance(cls,list) else [cls]
    class_names=','.join(f'{PKG}.{name}' for name in classes)
    args=['shell','am','instrument','-w','-r','-e','class',class_names]
    for k,v in kwargs.items(): args+=['-e',k,str(v)]
    args+=[f'{PKG}.test/androidx.test.runner.AndroidJUnitRunner']
    result=adb(*args,timeout=240)
    print(result)
    if any(marker in result for marker in ['FAILURES!!!', 'INSTRUMENTATION_FAILED', 'INSTRUMENTATION_ABORTED', 'shortMsg=']):
        raise RuntimeError(f'Instrumentation failed: {cls}')
    return result

def clear_system_ui_dialog():
    # Software-rendered emulators can retain a System UI ANR from resizing.
    # Dismiss only that system dialog, never an application failure dialog.
    adb('shell','uiautomator','dump','/sdcard/store-qa-window.xml')
    root=ET.fromstring(adb('shell','cat','/sdcard/store-qa-window.xml'))
    if any(n.get('text') == "System UI isn't responding" for n in root.iter('node')):
        wait=next(n for n in root.iter('node') if n.get('text')=='Wait')
        x1,y1,x2,y2=map(int,re.findall(r'\d+',wait.get('bounds')))
        adb('shell','input','tap',(x1+x2)//2,(y1+y2)//2)
        time.sleep(3)
        print('Dismissed a retained emulator System UI dialog.')

if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('action',choices=['install','test','native','capture','screen','wireless-info','open-wireless'])
    parser.add_argument('--serial',default='emulator-5554')
    parser.add_argument('--port')
    parser.add_argument('--prefix',default='phone')
    args=parser.parse_args(); SERIAL=args.serial
    if args.action in ('test','capture') and not args.port:
        parser.error('--port is required for the paired Wireless debugging connection')
    if not SERIAL.startswith('emulator-'): raise RuntimeError('Only an explicitly selected emulator is allowed by this QA script.')
    qa=ROOT/'output/qa'; qa.mkdir(parents=True,exist_ok=True)
    if args.action=='install':
        print(adb('install','-r',ROOT/'app/build/outputs/apk/debug/app-debug.apk'))
        print(adb('install','-r','-t',ROOT/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'))
    elif args.action=='test':
        result=instrument(['PairingLibraryTest','AppFlowTest','LocalStoreTest'])
        (qa/'app-tests.txt').write_text(result,encoding='utf-8')
        result=instrument('LocalAdbTest',liveAdb='true',connectPort=args.port)
        (qa/'adb-integration.txt').write_text(result,encoding='utf-8')
    elif args.action=='native':
        result=instrument('PairingLibraryTest')
        (qa/'native-pairing-tests.txt').write_text(result,encoding='utf-8')
    elif args.action=='capture':
        if args.prefix=='phone':
            print(adb('shell','wm','size','720x1600')); print(adb('shell','wm','density','280'))
        elif args.prefix in ('tablet','tablet7'):
            print(adb('shell','wm','size','1440x900')); print(adb('shell','wm','density','240' if args.prefix=='tablet7' else '180'))
        else:
            print(adb('shell','wm','size','1920x1080')); print(adb('shell','wm','density','180'))
        time.sleep(3)
        clear_system_ui_dialog()
        result=instrument('StoreCaptureTest',captureStore='true',connectPort=args.port,capturePrefix=args.prefix)
        (qa/f'capture-{args.prefix}.txt').write_text(result,encoding='utf-8')
        out=ROOT/'store/raw'; out.mkdir(parents=True,exist_ok=True)
        for name in ['shell','commands','history','connect','settings']:
            print(adb('pull',f'/sdcard/Android/data/{PKG}/files/store-captures/{args.prefix}-{name}.png',out))
    elif args.action=='screen':
        (qa/'screen.png').write_bytes(adb('exec-out','screencap','-p',binary=True))
        print(qa/'screen.png')
    elif args.action=='wireless-info':
        print('Wireless debugging enabled:',adb('shell','settings','get','global','adb_wifi_enabled').strip())
        print('TLS connection port:',adb('shell','getprop','service.adb.tls.port').strip())
    elif args.action=='open-wireless':
        # This is the isolated development emulator, not the user's physical device.
        print(adb('shell','am','start','-a','android.settings.APPLICATION_DEVELOPMENT_SETTINGS'))
