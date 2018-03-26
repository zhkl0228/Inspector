#!/system/xbin/env bash

if [ ! -x "/data/data/com.fuzhu8.inspector/files/assets/armeabi-v7a/hijack" ]; then
    unzip -o -d /data/data/com.fuzhu8.inspector/files /data/app/com.fuzhu8.inspector-*.apk assets/armeabi-v7a/hijack
    chmod 755 /data/data/com.fuzhu8.inspector/files/assets/armeabi-v7a/hijack
fi

/data/data/com.fuzhu8.inspector/files/assets/armeabi-v7a/hijack -p $1 -l /data/data/com.fuzhu8.inspector/lib/liblauncher.so -d
