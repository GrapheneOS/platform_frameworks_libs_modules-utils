/*
 * Copyright (C) 2023 The Android Open Source Project
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
package android.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates an API is part of a feature that is guarded by an aconfig flag.
 * </p>
 * This annotation should only appear on APIs that are marked <pre>@hide</pre>.
 *
 * @hide
 */
@Target({TYPE, METHOD, CONSTRUCTOR})
@Retention(RetentionPolicy.SOURCE)
public @interface FlaggedApi {
    /**
     * Namespace and name of aconfig flag used to guard the feature this API is part of. Expected
     * syntax: namespace/name, e.g. "the_namespace/the_name_of_the_flag".
     */
    String value();
}
