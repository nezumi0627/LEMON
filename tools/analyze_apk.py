#!/usr/bin/env python3
"""
LEMON🍋 LINE APK Analyzer
LINEのAPKを解析して、リソースや文字列などを確認するツール。
apktool.jar が必要です。事前に配置してください。
Usage: python analyze_apk.py
"""

import subprocess
import os
import sys
import shutil
import zipfile
import json

# ============================================================
# 設定 (Config)
# ============================================================
APK_PATH = "line_base.apk"
OUTPUT_DIR = "line_extracted"
APKTOOL_JAR = "apktool.jar"
APKTOOL_URL = "https://bitbucket.org/iBotPeaches/apktool/downloads/apktool_2.9.3.jar"
# ============================================================

def check_java():
    """Javaが利用可能かチェック"""
    try:
        result = subprocess.run(["java", "-version"], capture_output=True, text=True)
        print(f"✅ Java detected: {result.stderr.strip().splitlines()[0]}")
        return True
    except FileNotFoundError:
        print("❌ Java not found. Please install Java 17+")
        return False

def download_apktool():
    """apktoolが存在しない場合はダウンロードガイドを表示"""
    if not os.path.exists(APKTOOL_JAR):
        print(f"\n⚠️  apktool.jar not found.")
        print(f"   Download from: {APKTOOL_URL}")
        print(f"   Place it at: {os.path.abspath(APKTOOL_JAR)}")
        return False
    return True

def decompile_apk():
    """apktoolでAPKをデコンパイル"""
    if not os.path.exists(APK_PATH):
        print(f"❌ APK not found: {APK_PATH}")
        print("   Run: adb pull /data/app/.../base.apk line_base.apk")
        return False

    if os.path.exists(OUTPUT_DIR):
        print(f"⚠️  Output dir exists. Removing: {OUTPUT_DIR}")
        shutil.rmtree(OUTPUT_DIR)

    print(f"\n🔍 Decompiling {APK_PATH} → {OUTPUT_DIR} ...")
    result = subprocess.run(
        ["java", "-jar", APKTOOL_JAR, "d", APK_PATH, "-o", OUTPUT_DIR, "--no-res"],
        capture_output=True, text=True
    )
    if result.returncode != 0:
        print(f"❌ apktool failed:\n{result.stderr}")
        return False
    print("✅ Decompile completed!")
    return True

def search_strings(keyword):
    """デコンパイル済みファイルからキーワードを含む文字列を検索"""
    found = []
    for root, dirs, files in os.walk(OUTPUT_DIR):
        # .gitフォルダは除外
        dirs[:] = [d for d in dirs if d != ".git"]
        for fname in files:
            if fname.endswith((".xml", ".smali", ".json", ".properties")):
                fpath = os.path.join(root, fname)
                try:
                    with open(fpath, "r", encoding="utf-8", errors="ignore") as f:
                        for i, line in enumerate(f, 1):
                            if keyword.lower() in line.lower():
                                found.append({"file": fpath, "line": i, "content": line.strip()})
                except Exception:
                    pass
    return found

def list_activities():
    """AndroidManifest.xmlからActivity一覧を抽出"""
    manifest = os.path.join(OUTPUT_DIR, "AndroidManifest.xml")
    if not os.path.exists(manifest):
        return []
    activities = []
    with open(manifest, "r", encoding="utf-8", errors="ignore") as f:
        for line in f:
            if "Activity" in line and 'name=' in line:
                activities.append(line.strip())
    return activities

def extract_strings_xml():
    """strings.xmlから対象文字列を抽出（ZIP直接展開版）"""
    print("\n📦 Extracting strings.xml from APK directly...")
    results = {}
    try:
        with zipfile.ZipFile(APK_PATH, 'r') as z:
            xml_files = [f for f in z.namelist() if "strings" in f and f.endswith(".xml")]
            for xml_file in xml_files:
                content = z.read(xml_file).decode("utf-8", errors="ignore")
                results[xml_file] = content[:2000]  # 先頭2000文字
    except Exception as e:
        print(f"  ⚠️  Direct zip extraction failed: {e}")
    return results


def main():
    print("=" * 60)
    print("🍋 LEMON APK Analyzer")
    print("=" * 60)

    if not check_java():
        sys.exit(1)

    # ==============================
    # モード選択
    # ==============================
    print("\nMode:")
    print("  1. Full Decompile (apktool) + Search")
    print("  2. Quick String Search (requires already decompiled)")
    print("  3. Extract strings.xml from APK directly")
    print("  4. List Activities (requires already decompiled)")
    choice = input("\nChoose (1-4): ").strip()

    if choice == "1":
        if not download_apktool():
            sys.exit(1)
        if not decompile_apk():
            sys.exit(1)
        keyword = input("Search keyword (e.g. 'LINEへようこそ'): ").strip()
        results = search_strings(keyword)
        print(f"\n🔍 Found {len(results)} matches for '{keyword}':")
        for r in results[:50]:
            print(f"  [{r['line']}] {r['file']}\n    → {r['content']}")

    elif choice == "2":
        if not os.path.exists(OUTPUT_DIR):
            print(f"❌ {OUTPUT_DIR} not found. Run decompile first (choice 1).")
            sys.exit(1)
        keyword = input("Search keyword: ").strip()
        results = search_strings(keyword)
        print(f"\n🔍 Found {len(results)} matches for '{keyword}':")
        for r in results[:50]:
            print(f"  [{r['line']}] {r['file']}\n    → {r['content']}")

    elif choice == "3":
        strings = extract_strings_xml()
        if strings:
            print(f"\n✅ Found {len(strings)} strings.xml variants:")
            for path, content in strings.items():
                print(f"\n--- {path} ---\n{content[:500]}\n...")
        else:
            print("No strings.xml found in APK.")

    elif choice == "4":
        if not os.path.exists(OUTPUT_DIR):
            print(f"❌ {OUTPUT_DIR} not found. Run decompile first (choice 1).")
            sys.exit(1)
        activities = list_activities()
        print(f"\n📋 Activities ({len(activities)}):")
        for a in activities:
            print(f"  {a}")
    else:
        print("Invalid choice.")

    print("\n✅ Done!")


if __name__ == "__main__":
    main()
