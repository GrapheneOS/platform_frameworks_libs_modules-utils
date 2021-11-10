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

    /**
     * Checks if the device is running on a given or older version of Android.
     */
    public static boolean isAtMost(@NonNull String version) {
        return sInstance.isAtMostInternal(version);
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
            return mSdkInt >= Integer.parseInt(version);
        }
        if (isCodename(version)) {
            return mCodename.compareTo(version) >= 0;
        }
        // Never assume what the next SDK level is until SDK finalization completes.
        // SDK_INT is always assigned the latest finalized value of the SDK.
        return mSdkInt >= Integer.parseInt(version);
    }

    @VisibleForTesting
    boolean isAtMostInternal(@NonNull String version) {
        if (mIsReleaseBuild) {
            // On release builds we only expect to install artifacts meant for released
            // Android Versions. No codenames.
            return mSdkInt <= Integer.parseInt(version);
        }
        if (isCodename(version)) {
            return mCodename.compareTo(version) <= 0;
        }
        // Never assume what the next SDK level is until SDK finalization completes.
        // SDK_INT is always assigned the latest finalized value of the SDK.
        //
        // Note: multiple releases can be in development at the same time. For example, during
        // Sv2 and Tiramisu development, both builds have SDK_INT=31 which is not a sufficient
        // information to differentiate between them. Also, "31" at that point already corresponds
        // to a previously finalized API level, meaning that the current build is not at most "31".
        // This is why the comparison is strict, instead of <=.
        return mSdkInt < Integer.parseInt(version);
    }

    private boolean isCodename(String version) {
        if (version.length() == 0) {
            throw new IllegalArgumentException();
        }
        // assume Android codenames start with upper case letters.
        return Character.isUpperCase((version.charAt(0)));
    }

}
