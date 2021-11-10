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

#include <android-base/logging.h>
#include <android-base/properties.h>
#include <android/api-level.h>

namespace android {
namespace modules {
namespace sdklevel {
namespace unbounded {

static inline auto getVersionInt(const std::string &version) {
  std::size_t processed_chars = 0;
  const int versionInt = std::stoi(version, &processed_chars);
  CHECK(processed_chars == version.size());
  return versionInt;
}

static inline bool isCodename(const std::string &version) {
  CHECK(version.size() > 0);
  return std::isupper(version.at(0));
}

// Checks if the device is running a specific version or newer.
// Always use specific methods IsAtLeast*() available in sdk_level.h when the
// version is known at build time. This should only be used when a dynamic
// runtime check is needed.
static inline bool IsAtLeast(const std::string &version) {
  const std::string &device_codename =
      android::base::GetProperty("ro.build.version.codename", "");
  if ("REL" == device_codename) {
    return android_get_device_api_level() >= getVersionInt(version);
  }
  if (isCodename(version)) {
    return device_codename.compare(version) >= 0;
  }
  return android_get_device_api_level() >= getVersionInt(version);
}

// Checks if the device is running a specific version or older.
// Always use specific methods IsAtLeast*() available in sdk_level.h when the
// version is known at build time. This should only be used when a dynamic
// runtime check is needed.
static inline bool IsAtMost(const std::string &version) {
  const std::string &device_codename =
      android::base::GetProperty("ro.build.version.codename", "");
  if ("REL" == device_codename) {
    return android_get_device_api_level() <= getVersionInt(version);
  }
  if (isCodename(version)) {
    return device_codename.compare(version) <= 0;
  }
  return android_get_device_api_level() < getVersionInt(version);
}

} // namespace unbounded
} // namespace sdklevel
} // namespace modules
} // namespace android
