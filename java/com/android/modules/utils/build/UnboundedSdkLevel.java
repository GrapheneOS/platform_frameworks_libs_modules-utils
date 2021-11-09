/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.modules.utils.build;

import android.os.Build;

import androidx.annotation.NonNull;

import com.android.internal.annotations.VisibleForTesting;

/**
 * Utility class to check SDK level on a device.
 *
 * <p>Prefer using {@link SdkLevel} if the version is known at build time. This should only be used
 * when a dynamic runtime check is needed.
 */
public final class UnboundedSdkLevel {

    /**
     * Checks if the device is running on a given or newer version of Android.
     */
    public static boolean isAtLeast(@NonNull String version) {
        return sInstance.isAtLeastInternal(version);
    }

    private static final UnboundedSdkLevel sInstance =
            new UnboundedSdkLevel(Build.VERSION.SDK_INT, Build.VERSION.CODENAME);

    private final int mSdkInt;
    private final String mCodename;
    private final boolean mIsReleaseBuild;

    @VisibleForTesting
    UnboundedSdkLevel(int sdkInt, String codename) {
        mSdkInt = sdkInt;
        mCodename = codename;
        mIsReleaseBuild = "REL".equals(codename);
    }

    @VisibleForTesting
    boolean isAtLeastInternal(@NonNull String version) {
        if (mIsReleaseBuild) {
            // On release builds we only expect to install artifacts meant for released
            // Android Versions. No codenames.
            int versionInt = Integer.parseInt(version);
            return mSdkInt >= versionInt;
        }
        if (version.length() == 0) {
            throw new IllegalArgumentException();
        }
        // Assume Android codenames do not start with a digit
        if (Character.isDigit(version.charAt(0))) {
            // Never assume what the next SDK level is until SDK finalization completes.
            // SDK_INT is always assigned the latest finalized value of the SDK.
            int versionInt = Integer.parseInt(version);
            return mSdkInt >= versionInt;
        }
        return mCodename.compareTo(version) >= 0;
    }

}
