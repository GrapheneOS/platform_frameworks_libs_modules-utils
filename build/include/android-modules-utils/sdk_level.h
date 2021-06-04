/*
 * Copyright (C) 2020 The Android Open Source Project
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

#pragma once

#include <android-base/properties.h>

namespace android {
namespace modules {
namespace sdklevel {

// Return true iff the running Android SDK is at least "R".
static inline bool IsAtLeastR() {
  return android::base::GetIntProperty("ro.build.version.sdk", -1) >= 30;
}

// Return true iff the running Android SDK is at least "S".
static inline bool IsAtLeastS() {
  return android::base::GetIntProperty("ro.build.version.sdk", -1) >= 31;
}

} // namespace utils
} // namespace modules
} // namespace android
