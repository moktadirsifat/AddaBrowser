#!/usr/bin/env python3
"""Small Android 11 UI smoke test for the v1.16 browser shell.

This intentionally checks only stable native UI flows. It does not claim to
replace physical-device testing or full browser navigation/gesture regression.
"""
import subprocess
import tempfile
import time
import xml.etree.ElementTree as ET


def adb(*args):
    return subprocess.run(
        ["adb", *args], check=True, text=True,
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT
    ).stdout


def hierarchy():
    adb("shell", "uiautomator", "dump", "/sdcard/sifat-window.xml")
    with tempfile.NamedTemporaryFile(suffix=".xml") as tmp:
        adb("pull", "/sdcard/sifat-window.xml", tmp.name)
        return ET.parse(tmp.name).getroot().iter("node")


def nodes():
    return list(hierarchy())


def labels():
    return [
        (n.attrib.get("text", ""), n.attrib.get("content-desc", ""))
        for n in nodes()
    ]


def wait_for(needle, exact=False, timeout=12):
    deadline = time.time() + timeout
    while time.time() < deadline:
        for text, desc in labels():
            values = (text, desc)
            if any((v == needle if exact else needle in v) for v in values):
                return
        time.sleep(0.6)
    raise AssertionError(f"UI text not found: {needle!r}; current labels={labels()}")


def tap(needle, exact=False, timeout=12):
    deadline = time.time() + timeout
    while time.time() < deadline:
        for node in nodes():
            text = node.attrib.get("text", "")
            desc = node.attrib.get("content-desc", "")
            matched = (text == needle or desc == needle) if exact else (
                needle in text or needle in desc
            )
            if matched and node.attrib.get("clickable") == "true":
                bounds = node.attrib["bounds"]
                left_top, right_bottom = bounds[1:-1].split("][")
                x1, y1 = map(int, left_top.split(","))
                x2, y2 = map(int, right_bottom.split(","))
                adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
                time.sleep(1)
                return
        time.sleep(0.5)
    raise AssertionError(f"Clickable UI element not found: {needle!r}; labels={labels()}")


# Fresh emulator install should start with one Home tab.
wait_for("1", exact=True)
print("PASS: initial single-tab counter is visible")

# Create a second tab, open the tab chooser, and close the active tab.
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

# Check that the menu opens and the empty-history state is handled cleanly.
tap("⋮", exact=True)
wait_for("History")
tap("History")
wait_for("Recent history")
wait_for("Nothing here yet.")
print("PASS: menu opens History and the empty-history state is readable")
tap("OK", exact=True)
print("Android 11 UI smoke regression completed successfully.")
