#!/bin/bash
#
# Copyright (C) 2016 The CyanogenMod Project
# Copyright (C) 2017 The LineageOS Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e

# Load extract_utils and do some sanity checks
MY_DIR="${BASH_SOURCE%/*}"
if [[ ! -d "$MY_DIR" ]]; then MY_DIR="$PWD"; fi

ROM_ROOT="$MY_DIR"/../../..

HELPER="$ROM_ROOT"/buildtools/extract_utils.sh
if [ ! -f "$HELPER" ]; then
    echo "Unable to find helper script at $HELPER"
    exit 1
fi
. "$HELPER"

# Default to sanitizing the vendor folder before extraction
CLEAN_VENDOR=true

while [ "$1" != "" ]; do
    case $1 in
        -n | --no-cleanup )     CLEAN_VENDOR=false
                                ;;
        -s | --section )        shift
                                SECTION=$1
                                CLEAN_VENDOR=false
                                ;;
        * )                     SRC=$1
                                ;;
    esac
    shift
done

if [ -z "$SRC" ]; then
    SRC=adb
fi

# Initialize the helper for common device
setup_vendor "$DEVICE_COMMON" "$VENDOR" "$ROM_ROOT" true "$CLEAN_VENDOR"

extract "$MY_DIR"/proprietary-files.txt "$SRC" "$SECTION"

if [ -s "$MY_DIR"/../$DEVICE/proprietary-files.txt ]; then
    # Reinitialize the helper for device
    setup_vendor "$DEVICE" "$VENDOR" "$ROM_ROOT" false "$CLEAN_VENDOR"

    extract "$MY_DIR"/../$DEVICE/proprietary-files.txt "$SRC" "$SECTION"
fi

COMMON_BLOB_ROOT="$ROM_ROOT"/vendor/"$VENDOR"/"$DEVICE_COMMON"/proprietary

#
# Load camera configs from vendor
#
CAMERA2_SENSOR_MODULES="$COMMON_BLOB_ROOT"/vendor/lib/libmmcamera2_sensor_modules.so
sed -i "s|/system/etc/camera/|/vendor/etc/camera/|g" "$CAMERA2_SENSOR_MODULES"

#
# Load camera watermark from vendor
#
MI_CAMERA_HAL="$COMMON_BLOB_ROOT"/vendor/lib/libMiCameraHal.so
sed -i "s|system/etc/dualcamera.png|vendor/etc/dualcamera.png|g" "$MI_CAMERA_HAL"

#
# Replace libicuuc.so with libicuuc-v27.so for libMiCameraHal.so
#
ICUUC_V27="$COMMON_BLOB_ROOT"/vendor/lib/libicuuc-v27.so
patchelf --replace-needed libicuuc.so libicuuc-v27.so "$MI_CAMERA_HAL"
patchelf --set-soname libicuuc-v27.so "$ICUUC_V27"

#
# Replace libminikin.so with libminikin-v27.so for camera.msm8998.so
#
CAMERA_MSM8998="$COMMON_BLOB_ROOT"/vendor/lib/hw/camera.msm8998.so
MINIKIN_V27="$COMMON_BLOB_ROOT"/vendor/lib/libminikin-v27.so
patchelf --replace-needed libminikin.so libminikin-v27.so "$CAMERA_MSM8998"
patchelf --set-soname libminikin-v27.so "$MINIKIN_V27"

patchelf --remove-needed vendor.xiaomi.hardware.mtdservice@1.0.so "$BLOB_ROOT"/vendor/bin/mlipayd
patchelf --remove-needed vendor.xiaomi.hardware.mtdservice@1.0.so "$BLOB_ROOT"/vendor/lib64/libmlipay.so

"$MY_DIR"/setup-makefiles.sh
