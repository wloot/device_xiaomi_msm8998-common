/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "android.hardware.thermal@1.2-service.xiaomi_msm8998"

#include <android/log.h>
#include <hidl/HidlTransportSupport.h>
#include "Thermal.h"

using android::sp;
using android::status_t;
using android::OK;

using android::hardware::configureRpcThreadpool;
using android::hardware::joinRpcThreadpool;

using android::hardware::thermal::V1_1::IThermal;
using android::hardware::thermal::V1_1::implementation::Thermal;

int main(int /* argc */, char** /* argv */) {
    ALOGI("Thermal HAL Service 1.1 is starting");

    android::sp<IThermal> service = new Thermal();
    if (service == nullptr) {
        ALOGE("Could not create an instance of Thermal HAL");
        return 1;
    }

    configureRpcThreadpool(1, true /* callerWillJoi n*/);

    status_t status = service->registerAsService();
    if (status != OK) {
        ALOGE("Could not register Thermal HAL service");
        return 1;
    }

    ALOGI("Thermal HAL service is ready");
    joinRpcThreadpool();

    // In normal operation, we don't expect the thread pool to exit
    ALOGE("Thermal HAL service is shutting down");
    return 1;
}
