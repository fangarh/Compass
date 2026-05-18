# Compass Recovery Tools

This directory keeps local reverse-engineering tools used to recover `compassv33.apk`.

- `apktool_3.0.2.jar`: decodes Android resources, manifest, and smali.
- `jadx-1.5.5/`: decompiles DEX into Java-like source for manual porting.

The generated recovery output lives under `recovered/` and should be treated as source material, not as clean application code.
