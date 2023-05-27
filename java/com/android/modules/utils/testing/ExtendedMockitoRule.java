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

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;
import org.mockito.MockitoFramework;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Rule to make it easier to use Extended Mockito:
 *
 * <ul>
 *   <li>Automatically creates and finishes the mock session.
 *   <li>Provides multiple ways to set which classes must be statically mocked or spied
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
public class ExtendedMockitoRule implements TestRule {

    private static final String TAG = ExtendedMockitoRule.class.getSimpleName();

    private final Object mTestClassInstance;
    private final Strictness mStrictness;
    private @Nullable final MockitoFramework mMockitoFramework;
    private @Nullable final Runnable mAfterSessionFinishedCallback;
    private final List<Class<?>> mMockedStaticClasses;
    private final List<Class<?>> mSpiedStaticClasses;
    private final @Nullable SessionBuilderVisitor mSessionBuilderConfigurator;
    private final boolean mClearInlineMocks;

    private MockitoSession mMockitoSession;

    protected ExtendedMockitoRule(Builder builder) {
        mTestClassInstance = builder.mTestClassInstance;
        mStrictness = builder.mStrictness;
        mMockitoFramework = builder.mMockitoFramework;
        mMockitoSession = builder.mMockitoSession;
        mAfterSessionFinishedCallback = builder.mAfterSessionFinishedCallback;
        mSessionBuilderConfigurator = builder.mSessionBuilderConfigurator;
        mMockedStaticClasses = builder.mMockedStaticClasses;
        mSpiedStaticClasses = builder.mSpiedStaticClasses;
        mClearInlineMocks = builder.mClearInlineMocks;
        Log.v(TAG, "strictness=" + mStrictness + ", testClassInstance" + mTestClassInstance
                + ", mockedStaticClasses=" + mMockedStaticClasses
                + ", spiedStaticClasses=" + mSpiedStaticClasses
                + ", mSessionBuilderConfigurator=" + mSessionBuilderConfigurator
                + ", afterSessionFinishedCallback=" + mAfterSessionFinishedCallback
                + ", mockitoFramework=" + mMockitoFramework
                + ", mockitoSession=" + mMockitoSession
                + ", clearInlineMocks=" + mClearInlineMocks);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        createMockitoSession();

        // TODO(b/281577492): will need to provide a afterSessionCreated() callback in order to
        // change StaticMockFixtureRule to use it

        return new TestWatcher() {
            @Override
            protected void succeeded(Description description) {
                tearDown(description, /* e=*/ null);
            }

            @Override
            protected void skipped(AssumptionViolatedException e, Description description) {
                tearDown(description, e);
            }

            @Override
            protected void failed(Throwable e, Description description) {
                tearDown(description, e);
            }
        }.apply(base, description);
    }

    private void createMockitoSession() {
        Log.v(TAG, "Creating (and initializing mocks on) session builder with strictness "
                + mStrictness);
        StaticMockitoSessionBuilder sessionBuilder = mockitoSession().strictness(mStrictness)
                .initMocks(mTestClassInstance);

        setUpMockedClasses(sessionBuilder);

        if (mMockitoSession != null) {
            Log.d(TAG, "NOT creating session as set on builder: " + mMockitoSession);
        } else {
            Log.v(TAG, "Creating mockito session using " + sessionBuilder);
            mMockitoSession = sessionBuilder.startMocking();
        }
    }

    private void setUpMockedClasses(StaticMockitoSessionBuilder sessionBuilder) {
        mMockedStaticClasses.forEach((c) -> sessionBuilder.mockStatic(c));
        mSpiedStaticClasses.forEach((c) -> sessionBuilder.spyStatic(c));
        if (mSessionBuilderConfigurator != null) {
            Log.v(TAG, "Visiting " + mSessionBuilderConfigurator + " with " + sessionBuilder);
            mSessionBuilderConfigurator.visit(sessionBuilder);
        }
    }

    private void tearDown(Description description, Throwable e) {
        Log.v(TAG, "Finishing mockito session " + mMockitoSession + " on " + description
                + " (e=" + e + ")");
        try {
            try {
                mMockitoSession.finishMocking(e);
                mMockitoSession = null;
            } finally {
                if (mAfterSessionFinishedCallback != null) {
                    mAfterSessionFinishedCallback.run();
                }
            }
        } finally {
            clearInlineMocks();
        }
    }

    private void clearInlineMocks() {
        if (!mClearInlineMocks) {
            Log.d(TAG, "NOT calling clearInlineMocks() as set on builder");
            return;
        }
        if (mMockitoFramework != null) {
            Log.v(TAG, "Calling clearInlineMocks() on custom mockito framework: "
                    + mMockitoFramework);
            mMockitoFramework.clearInlineMocks();
            return;
        }
        Log.v(TAG, "Calling Mockito.framework().clearInlineMocks()");
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Builder for the rule.
     */
    public static final class Builder {
        private final Object mTestClassInstance;
        private final List<Class<?>> mMockedStaticClasses = new ArrayList<>();
        private final List<Class<?>> mSpiedStaticClasses = new ArrayList<>();
        private Strictness mStrictness = Strictness.LENIENT;
        private @Nullable MockitoFramework mMockitoFramework;
        private @Nullable MockitoSession mMockitoSession;
        private @Nullable SessionBuilderVisitor mSessionBuilderConfigurator;
        private @Nullable Runnable mAfterSessionFinishedCallback;
        private boolean mClearInlineMocks = true;

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

        // TODO(b/281577492): remove if / when it supports StaticMockFixture, as it's only used by
        // CachedAppOptimizerTest
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
         * Runs the given {@code runnable} after the session finished.
         *
         * <p>Typically used for clean-up code that cannot be executed on {@code &#064;After}, as
         * those methods are called before the session is finished.
         */
        public Builder afterSessionFinished(Runnable runnable) {
            mAfterSessionFinishedCallback = Objects.requireNonNull(runnable);
            return this;
        }

        /**
         * By default, it cleans up inline mocks after the session is closed to prevent OutOfMemory
         * errors (see <a href="https://github.com/mockito/mockito/issues/1614">external bug</a>
         * and/or <a href="http://b/259280359">internal bug</a>); use this method to not do so.
         */
        public Builder dontClearInlineMocks() {
            mClearInlineMocks  = false;
            return this;
        }

        // Used mostly by ExtendedMockitoRuleTest itself
        @VisibleForTesting
        Builder setMockitoFrameworkForTesting(MockitoFramework mockitoFramework) {
            mMockitoFramework = Objects.requireNonNull(mockitoFramework);
            return this;
        }

        // Used mostly by ExtendedMockitoRuleTest itself
        @VisibleForTesting
        Builder setMockitoSessionForTesting(MockitoSession mockitoSession) {
            mMockitoSession = Objects.requireNonNull(mockitoSession);
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
