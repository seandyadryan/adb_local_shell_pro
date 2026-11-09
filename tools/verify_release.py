"""Inspect signed outputs, their checksums, and native 16 KB compatibility."""
from pathlib import Path
import os,sys,subprocess,hashlib,json,struct,zipfile

ROOT=Path(__file__).resolve().parents[1]
SDK=Path(os.environ.get('ANDROID_HOME',Path(os.environ.get('LOCALAPPDATA',''))/'Android/Sdk'))
BUILD=SDK/'build-tools/36.0.0'

def run(args):
    result=subprocess.run(list(map(str,args)),capture_output=True,text=True)
    if result.returncode: raise RuntimeError(result.stdout+result.stderr)
    return result.stdout+result.stderr

def elf_alignment(data):
    assert data[:4]==b'\x7fELF'
    is64=data[4]==2; endian='<' if data[5]==1 else '>'
    off=struct.unpack_from(endian+('Q' if is64 else 'I'),data,32 if is64 else 28)[0]
    entry,count=struct.unpack_from(endian+'HH',data,54 if is64 else 42)
    loads=[]
    for i in range(count):
        p=off+i*entry
        if struct.unpack_from(endian+'I',data,p)[0]==1:
            loads.append(struct.unpack_from(endian+('Q' if is64 else 'I'),data,p+(48 if is64 else 28))[0])
    assert loads and min(loads)>=16384,loads
    return loads

if __name__=='__main__':
    out=ROOT/'output/release';out.mkdir(parents=True,exist_ok=True)
    apk=ROOT/'app/build/outputs/apk/release/app-release.apk'
    aab=ROOT/'app/build/outputs/bundle/release/app-release.aab'
    results={}
    results['apk_signature']=run([BUILD/'apksigner.bat' if os.name=='nt' else BUILD/'apksigner','verify','--verbose','--print-certs',apk])
    results['zip_alignment']=run([BUILD/('zipalign.exe' if os.name=='nt' else 'zipalign'),'-c','-P','16','-v','4',apk]).splitlines()[-1]
    results['bundle_signature']=run(['jarsigner','-verify',aab])
    bundletool=ROOT/'.tools/bundletool-all-1.18.2.jar'
    if bundletool.exists():
        results['bundletool_validation']=run(['java','-Xmx256m','-jar',bundletool,'validate','--bundle',aab])
        config=run(['java','-Xmx256m','-jar',bundletool,'dump','config','--bundle',aab])
        assert 'PAGE_ALIGNMENT_16K' in config, 'AAB must request 16 KB APK alignment.'
        results['bundle_page_alignment']='PAGE_ALIGNMENT_16K'
    results['native_libraries']=[]
    with zipfile.ZipFile(apk) as z:
        for name in z.namelist():
            if name.endswith('.so'):
                align=elf_alignment(z.read(name))
                results['native_libraries'].append({'file':name,'load_segment_alignments':align,'uncompressed':z.getinfo(name).compress_type==0})
    with zipfile.ZipFile(aab) as z:
        assert 'BundleConfig.pb' in z.namelist() and 'base/manifest/AndroidManifest.xml' in z.namelist()
        bundle_libs=[name for name in z.namelist() if name.startswith('base/lib/') and name.endswith('.so')]
        assert {name.removeprefix('base/') for name in bundle_libs} == {lib['file'] for lib in results['native_libraries']}
        for name in bundle_libs:
            elf_alignment(z.read(name))
    for source,name in [(apk,'adb-local-shell-pro-1.0.0.apk'),(aab,'adb-local-shell-pro-1.0.0.aab')]:
        import shutil
        dest=out/name;shutil.copy2(source,dest)
        results[name]={'bytes':dest.stat().st_size,'sha256':hashlib.sha256(dest.read_bytes()).hexdigest()}
    badging=run([BUILD/('aapt.exe' if os.name=='nt' else 'aapt'),'dump','badging',apk])
    assert 'application-debuggable' not in badging, 'Release APK must not be debuggable.'
    results['debuggable']=False
    results['package_info']=badging.splitlines()[:8]
    expected="package: name='com.deploydulupulangnanti.adblocalshellpro' versionCode='1' versionName='1.0.0'"
    assert results['package_info'][0].startswith(expected)
    assert "sdkVersion:'30'" in results['package_info'] and "targetSdkVersion:'36'" in results['package_info']
    (out/'verification.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
    sums='\n'.join(f"{results[name]['sha256']}  {name}" for name in ['adb-local-shell-pro-1.0.0.aab','adb-local-shell-pro-1.0.0.apk'])+'\n'
    (out/'SHA256SUMS.txt').write_text(sums,encoding='utf-8')
    print(f"Verified APK/AAB signatures and all {len(results['native_libraries'])} native libraries with 16 KB alignment.")
