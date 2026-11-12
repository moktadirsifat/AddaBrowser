#!/usr/bin/env python3
"""Android 11 UI smoke test with recovery for transient emulator ANR dialogs."""
import subprocess
import tempfile
import time
import xml.etree.ElementTree as ET


def adb(*args):
    return subprocess.run(
        ["adb", *args], check=True, text=True,
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=30
    ).stdout


def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/sifat-window.xml")
    with tempfile.NamedTemporaryFile(suffix=".xml") as tmp:
        adb("pull", "/sdcard/sifat-window.xml", tmp.name)
        return list(ET.parse(tmp.name).getroot().iter("node"))


def labels():
    return [
        (n.attrib.get("text", ""), n.attrib.get("content-desc", ""))
        for n in nodes()
    ]


def tap_node(node):
    bounds = node.attrib.get("bounds")
    if not bounds:
        return False
    left_top, right_bottom = bounds[1:-1].split("][")
    x1, y1 = map(int, left_top.split(","))
    x2, y2 = map(int, right_bottom.split(","))
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(3)
    return True


def recover_anr_dialog(current_nodes):
    all_text = " ".join(
        n.attrib.get("text", "") + " " + n.attrib.get("content-desc", "")
        for n in current_nodes
    ).lower()
    if "isn't responding" not in all_text and "not responding" not in all_text:
        return False
    for node in current_nodes:
        text = node.attrib.get("text", "").strip().lower()
        if text in ("wait", "wait for app"):
            print("Detected Android ANR dialog; choosing Wait and allowing emulator to recover.")
            return tap_node(node)
    return False


def wait_for(needle, exact=False, timeout=45):
    deadline = time.time() + timeout
    last_labels = []
    while time.time() < deadline:
        try:
            current_nodes = nodes()
            last_labels = [
                (n.attrib.get("text", ""), n.attrib.get("content-desc", ""))
                for n in current_nodes
            ]
            if recover_anr_dialog(current_nodes):
                continue
            for text, desc in last_labels:
                needle_norm = needle.casefold()
                if any((v.casefold() == needle_norm if exact else needle_norm in v.casefold()) for v in (text, desc)):
                    return
        except (subprocess.SubprocessError, ET.ParseError, OSError) as exc:
            print(f"Temporary emulator UI read failure: {exc}")
        time.sleep(1)
    raise AssertionError(
        f"UI text not found after {timeout}s: {needle!r}; current labels={last_labels}"
    )


def tap(needle, exact=False, timeout=30):
    deadline = time.time() + timeout
    last_labels = []
    while time.time() < deadline:
        try:
            current_nodes = nodes()
            last_labels = [
                (n.attrib.get("text", ""), n.attrib.get("content-desc", ""))
                for n in current_nodes
            ]
            if recover_anr_dialog(current_nodes):
                continue
            for node in current_nodes:
                text = node.attrib.get("text", "")
                desc = node.attrib.get("content-desc", "")
                needle_norm = needle.casefold()
                matched = (text.casefold() == needle_norm or desc.casefold() == needle_norm) if exact else (
                    needle_norm in text.casefold() or needle_norm in desc.casefold()
                )
                if matched and node.attrib.get("clickable") == "true":
                    tap_node(node)
                    return
        except (subprocess.SubprocessError, ET.ParseError, OSError) as exc:
            print(f"Temporary emulator UI read failure: {exc}")
        time.sleep(1)
    raise AssertionError(
        f"Clickable UI element not found after {timeout}s: {needle!r}; labels={last_labels}"
    )


# Allow Android 11 emulator services to settle after the app activity is started.
time.sleep(15)
wait_for("1", exact=True, timeout=60)
print("PASS: initial single-tab counter is visible")

tap("+", exact=True)
wait_for("2", exact=True)
print("PASS: adding a tab updates the counter to 2")
tap("2", exact=True)
wait_for("Tabs · 2")
wait_for("New tab")
wait_for("Close current")
print("PASS: tab chooser exposes its expected actions")
tap("Close current", exact=True)
wait_for("1", exact=True)
print("PASS: closing the active tab returns to one usable tab")

tap("⋮", exact=True)
wait_for("History")
tap("History")
wait_for("Recent history")
wait_for("Nothing here yet.")
print("PASS: menu opens History and the empty-history state is readable")
tap("OK", exact=True)
print("Android 11 UI smoke regression completed successfully.")
