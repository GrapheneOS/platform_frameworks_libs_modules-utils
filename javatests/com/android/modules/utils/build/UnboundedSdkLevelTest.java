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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class UnboundedSdkLevelTest {

    @Test
    public void testFinalizedSdk_S() {
        // Test against finalized S / 31 release
        // Note empty set of known codenames, since on S builds Build.VERSION.KNOWN_CODENAMES
        // does not exist. This is a realistic unit test reflecting S release builds.
        UnboundedSdkLevel sdkLevel = new UnboundedSdkLevel(31, "REL", Set.of());

        assertThat(sdkLevel.isAtLeastInternal("30")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("31")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("32")).isFalse();

        assertThat(sdkLevel.isAtLeastInternal("R")).isFalse();
        assertThat(sdkLevel.isAtLeastInternal("S")).isFalse();
        assertThat(sdkLevel.isAtLeastInternal("Sv2")).isFalse();
        assertThat(sdkLevel.isAtLeastInternal("Tiramisu")).isFalse();
        assertThat(sdkLevel.isAtLeastInternal("U")).isFalse();

        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal(""));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal("current"));

        assertThat(sdkLevel.isAtMostInternal("30")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("31")).isTrue();
        assertThat(sdkLevel.isAtMostInternal("32")).isTrue();

        assertThat(sdkLevel.isAtMostInternal("R")).isTrue();
        assertThat(sdkLevel.isAtMostInternal("S")).isTrue();
        assertThat(sdkLevel.isAtMostInternal("Sv2")).isTrue();
        assertThat(sdkLevel.isAtMostInternal("Tiramisu")).isTrue();
        assertThat(sdkLevel.isAtMostInternal("U")).isTrue();

        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal(""));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal("current"));
    }

    @Test
    public void testFinalizedSdk() {
        // Test against finalized S / 31 release
        UnboundedSdkLevel sdkLevel = new UnboundedSdkLevel(31, "REL", Set.of("Q", "R", "S"));

        assertThat(sdkLevel.isAtLeastInternal("30")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("31")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("32")).isFalse();

        // R and S must have been re-compiled and changed to integer after SDK finalization
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal("R"));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal("S"));

        // Future versions are S+
        assertThat(sdkLevel.isAtLeastInternal("Sv2")).isFalse();
        assertThat(sdkLevel.isAtLeastInternal("Tiramisu")).isFalse();
        assertThat(sdkLevel.isAtLeastInternal("U")).isFalse();

        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal(""));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal("current"));

        assertThat(sdkLevel.isAtMostInternal("30")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("31")).isTrue();
        assertThat(sdkLevel.isAtMostInternal("32")).isTrue();

        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal("R"));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal("S"));
        assertThat(sdkLevel.isAtMostInternal("Sv2")).isTrue();
        assertThat(sdkLevel.isAtMostInternal("Tiramisu")).isTrue();
        assertThat(sdkLevel.isAtMostInternal("U")).isTrue();

        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal(""));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal("current"));
    }

    @Test
    public void testNonFinalizedSdk_Sv2() {
        UnboundedSdkLevel sdkLevel = new UnboundedSdkLevel(31, "Sv2", Set.of("Q", "R", "S", "Sv2"));

        assertThat(sdkLevel.isAtLeastInternal("30")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("31")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("32")).isFalse();

        assertThat(sdkLevel.isAtLeastInternal("R")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("S")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("Sv2")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("Tiramisu")).isFalse();
        assertThat(sdkLevel.isAtLeastInternal("U")).isFalse();

        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal(""));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal("current"));

        assertThat(sdkLevel.isAtMostInternal("30")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("31")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("32")).isTrue();

        assertThat(sdkLevel.isAtMostInternal("R")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("S")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("Sv2")).isTrue();
        assertThat(sdkLevel.isAtMostInternal("Tiramisu")).isTrue();
        assertThat(sdkLevel.isAtMostInternal("U")).isTrue();

        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal(""));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal("current"));
    }

    @Test
    public void testNonFinalizedSdk_Tiramisu() {
        UnboundedSdkLevel sdkLevel = new UnboundedSdkLevel(31, "Tiramisu",
                Set.of("Q", "R", "S", "Sv2", "Tiramisu"));

        assertThat(sdkLevel.isAtLeastInternal("30")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("31")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("32")).isFalse();

        assertThat(sdkLevel.isAtLeastInternal("R")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("S")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("Sv2")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("Tiramisu")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("U")).isFalse();

        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal(""));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal("current"));

        assertThat(sdkLevel.isAtMostInternal("30")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("31")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("32")).isTrue();

        assertThat(sdkLevel.isAtMostInternal("R")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("S")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("Sv2")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("Tiramisu")).isTrue();
        assertThat(sdkLevel.isAtMostInternal("U")).isTrue();

        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal(""));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal("current"));
    }



}
