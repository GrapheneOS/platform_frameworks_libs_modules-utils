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
package com.android.modules.utils.testing;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession;

import android.annotation.Nullable;
import android.util.Log;

import com.android.dx.mockito.inline.extended.StaticMockitoSessionBuilder;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;

import org.mockito.Mockito;
import org.mockito.MockitoFramework;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Rule to make it easier to use Extended Mockito.
 *
 * <p>It's derived from {@link StaticMockFixtureRule}, with the additional features:
 *
 * <ul>
 *   <li>Easier to define which classes must be statically mocked or spied
 *   <li>Automatically starts mocking (so tests don't need a mockito runner or rule)
 *   <li>Automatically clears the inlined mocks at the end (to avoid OOM)
 *   <li>Allows other customization like strictness
 * </ul>
 * <p>Typical usage:
 * <pre class="prettyprint">
 * &#064;Rule
 * public final ExtendedMockitoRule mExtendedMockitoRule = new ExtendedMockitoRule.Builder(this)
 *     .spyStatic(SomeClassWithStaticMethodsToBeMocked)
 *     .build();
 * </pre>
 */
public class ExtendedMockitoRule extends StaticMockFixtureRule {

    private static final String TAG = ExtendedMockitoRule.class.getSimpleName();

    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Object mTestClassInstance;
    private final Strictness mStrictness;
    private @Nullable final MockitoFramework mMockitoFramework;

    private ExtendedMockitoRule(Builder builder) {
        super(() -> new SimpleStaticMockFixture(builder.mMockedStaticClasses,
                builder.mSpiedStaticClasses, builder.mSessionBuilderConfigurator,
                builder.mAfterSessionFinishedCallback, builder.mMockitoFramework));
        mTestClassInstance = builder.mTestClassInstance;
        mStrictness = builder.mStrictness;
        mMockitoFramework = builder.mMockitoFramework;
        if (DEBUG) {
            Log.d(TAG, "strictness=" + mStrictness + ", testClassInstance" + mTestClassInstance
                    + ", mockedStaticClasses=" + builder.mMockedStaticClasses
                    + ", spiedStaticClasses=" + builder.mSpiedStaticClasses
                    + ", mSessionBuilderConfigurator="
                    + builder.mSessionBuilderConfigurator
                    + ", afterSessionFinishedCallback=" + builder.mAfterSessionFinishedCallback
                    + ", mockitoFramework=" + builder.mMockitoFramework);
        }
    }

    @Override
    public StaticMockitoSessionBuilder getSessionBuilder() {
        StaticMockitoSessionBuilder sessionBuilder;
        if (mStrictness != null) {
            sessionBuilder = mockitoSession().strictness(mStrictness);
            if (DEBUG) {
                Log.d(TAG, "Setting strictness to " + mStrictness + " on " + sessionBuilder);
            }
        } else {
            sessionBuilder = super.getSessionBuilder();
        }
        if (DEBUG) {
            Log.d(TAG, "Initializing mocks on " + mTestClassInstance);
        }
        return sessionBuilder.initMocks(mTestClassInstance);
    }

    /**
     * Builder for the rule.
     */
    public static final class Builder {
        private final Object mTestClassInstance;
        private final List<Class<?>> mMockedStaticClasses = new ArrayList<>();
        private final List<Class<?>> mSpiedStaticClasses = new ArrayList<>();
        private @Nullable Strictness mStrictness;
        private @Nullable MockitoFramework mMockitoFramework;
        private @Nullable SessionBuilderVisitor mSessionBuilderConfigurator;
        private @Nullable Runnable mAfterSessionFinishedCallback;

        /**
         * Constructs a builder for the giving test instance (typically {@code this}).
         */
        public Builder(Object testClassInstance) {
            mTestClassInstance = Objects.requireNonNull(testClassInstance);
        }

        /**
         * Sets the mock strictness.
         */
        public Builder setStrictness(Strictness strictness) {
            mStrictness = Objects.requireNonNull(strictness);
            return this;
        }

        /**
         * Same as {@link
         * com.android.dx.mockito.inline.extended.StaticMockitoSessionBuilder#mockStatic(Class)}.
         *
         * @throws IllegalStateException if the same class was already passed to
         *   {@link #mockStatic(Class)} or {@link #spyStatic(Class)} or if
         *   {@link #configureSessionBuilder(SessionBuilderVisitor)} was called before.
         */
        public Builder mockStatic(Class<?> clazz) {
            checkConfigureSessionBuilderNotCalled();
            mMockedStaticClasses.add(checkClassNotMockedOrSpied(clazz));
            return this;
        }

        /**
         * Same as {@link
         * com.android.dx.mockito.inline.extended.StaticMockitoSessionBuilder#spyStatic(Class)}.
         *
         * @throws IllegalStateException if the same class was already passed to
         *   {@link #mockStatic(Class)} or {@link #spyStatic(Class)} or if
         *   {@link #configureSessionBuilder(SessionBuilderVisitor)} was called before.
         */
        public Builder spyStatic(Class<?> clazz) {
            checkConfigureSessionBuilderNotCalled();
            mSpiedStaticClasses.add(checkClassNotMockedOrSpied(clazz));
            return this;
        }

        /**
         * Alternative for {@link #spyStatic(Class)} / {@link #mockStatic(Class)}; typically used
         * when the same setup is shared by multiple tests.
         *
         * @throws IllegalStateException if {@link #mockStatic(Class)} or {@link #spyStatic(Class)}
         * was called before.
         */
        public Builder configureSessionBuilder(
                SessionBuilderVisitor sessionBuilderConfigurator) {
            Preconditions.checkState(mMockedStaticClasses.isEmpty(),
                    "mockStatic() already called");
            Preconditions.checkState(mSpiedStaticClasses.isEmpty(),
                    "spyStatic() already called");
            mSessionBuilderConfigurator = Objects.requireNonNull(sessionBuilderConfigurator);
            return this;
        }

        /**
         * Executes the given code after the callback finished.
         *
         * <p>Typically used for clean-up code that cannot be executed on <code>@After</code>
         */
        public Builder afterSessionFinished(Runnable runnable) {
            mAfterSessionFinishedCallback = Objects.requireNonNull(runnable);
            return this;
        }

        // Used mostly by ExtendedMockitoRuleTest itself
        @VisibleForTesting
        Builder mockitoFramework(MockitoFramework mockitoFramework) {
            mMockitoFramework = Objects.requireNonNull(mockitoFramework);
            return this;
        }

        /**
         * Builds the rule.
         */
        public ExtendedMockitoRule build() {
            return new ExtendedMockitoRule(this);
        }

        private void checkConfigureSessionBuilderNotCalled() {
            Preconditions.checkState(mSessionBuilderConfigurator == null,
                    "configureSessionBuilder() already called");
        }

        private Class<?> checkClassNotMockedOrSpied(Class<?> clazz) {
            Objects.requireNonNull(clazz);
            Preconditions.checkState(!mMockedStaticClasses.contains(clazz),
                    "class %s already mocked", clazz);
            Preconditions.checkState(!mSpiedStaticClasses.contains(clazz),
                    "class %s already spied", clazz);
            return clazz;
        }
    }

    private static final class SimpleStaticMockFixture implements StaticMockFixture {

        private final List<Class<?>> mMockedStaticClasses;
        private final List<Class<?>> mSpiedStaticClasses;
        @Nullable
        private final SessionBuilderVisitor mSessionBuilderConfigurator;
        @Nullable
        private final Runnable mAfterSessionFinishedCallback;
        @Nullable
        private final MockitoFramework mMockitoFramework;

        private SimpleStaticMockFixture(List<Class<?>> mockedStaticClasses,
                List<Class<?>> spiedStaticClasses,
                @Nullable SessionBuilderVisitor dynamicSessionBuilderConfigurator,
                @Nullable Runnable afterSessionFinishedCallback,
                @Nullable MockitoFramework mockitoFramework) {
            mMockedStaticClasses = mockedStaticClasses;
            mSpiedStaticClasses = spiedStaticClasses;
            mSessionBuilderConfigurator = dynamicSessionBuilderConfigurator;
            mAfterSessionFinishedCallback = afterSessionFinishedCallback;
            mMockitoFramework = mockitoFramework;
        }

        @Override
        public StaticMockitoSessionBuilder setUpMockedClasses(
                StaticMockitoSessionBuilder sessionBuilder) {
            mMockedStaticClasses.forEach((c) -> sessionBuilder.mockStatic(c));
            mSpiedStaticClasses.forEach((c) -> sessionBuilder.spyStatic(c));
            if (mSessionBuilderConfigurator != null) {
                if (DEBUG) {
                    Log.d(TAG, "Visiting " + mSessionBuilderConfigurator + " with "
                            + sessionBuilder);
                }
                mSessionBuilderConfigurator.visit(sessionBuilder);
            }
            return sessionBuilder;
        }

        @Override
        public void setUpMockBehaviors() {
        }

        @Override
        public void tearDown() {
            try {
                if (mAfterSessionFinishedCallback != null) {
                    mAfterSessionFinishedCallback.run();
                }
            } finally {
                // When using inline mock maker, clean up inline mocks to prevent OutOfMemory
                // errors. See https://github.com/mockito/mockito/issues/1614 and b/259280359.
                if (mMockitoFramework != null) {
                    if (DEBUG) {
                        Log.d(TAG, "Calling clearInlineMocks() on custom mockito framework: "
                                + mMockitoFramework);
                    }
                    mMockitoFramework.clearInlineMocks();
                } else {
                    if (DEBUG) {
                        Log.d(TAG, "Calling Mockito.framework().clearInlineMocks()");
                    }
                    Mockito.framework().clearInlineMocks();
                }
            }
        }
    }

    /**
     * Visitor for {@link StaticMockitoSessionBuilder}.
     */
    public interface SessionBuilderVisitor {

        /**
         * Visits it.
         */
        void visit(StaticMockitoSessionBuilder builder);
    }
}
