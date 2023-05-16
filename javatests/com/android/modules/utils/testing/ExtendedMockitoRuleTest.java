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

import static com.android.dx.mockito.inline.extended.ExtendedMockito.doReturn;

import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.util.Log;

import androidx.annotation.Nullable;

import com.android.dx.mockito.inline.extended.StaticMockitoSessionBuilder;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mock;
import org.mockito.MockitoFramework;
import org.mockito.exceptions.misusing.UnnecessaryStubbingException;
import org.mockito.invocation.InvocationFactory;
import org.mockito.listeners.MockitoListener;
import org.mockito.plugins.MockitoPlugins;
import org.mockito.quality.Strictness;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ExtendedMockitoRuleTest {

    public static final String TAG = ExtendedMockitoRuleTest.class.getSimpleName();

    private final Statement mStatement = mock(Statement.class);
    private final Description mDescription = mock(Description.class);

    private final ClassUnderTest mClassUnderTest = new ClassUnderTest();
    private final ExtendedMockitoRule.Builder mBuilder =
            new ExtendedMockitoRule.Builder(mClassUnderTest);

    private final MySessionBuilderVisitor mConfigurator =
            new MySessionBuilderVisitor();

    @Test
    public void testBuilder_constructor_null() {
        assertThrows(NullPointerException.class, () -> new ExtendedMockitoRule.Builder(null));
    }

    @Test
    public void testBuilder_setStrictness_null() {
        assertThrows(NullPointerException.class, () -> mBuilder.setStrictness(null));
    }

    @Test
    public void testBuilder_configureSessionBuilder_null() {
        assertThrows(NullPointerException.class,
                () -> mBuilder.configureSessionBuilder(null));
    }

    @Test
    public void testBuilder_mockStatic_null() {
        assertThrows(NullPointerException.class, () -> mBuilder.mockStatic(null));
    }

    @Test
    public void testBuilder_spyStatic_null() {
        assertThrows(NullPointerException.class, () -> mBuilder.spyStatic(null));
    }

    @Test
    public void testBuilder_afterSessionFinished_null() {
        assertThrows(NullPointerException.class, () -> mBuilder.afterSessionFinished(null));
    }

    @Test
    public void testBuilder_mockitoFramework_null() {
        assertThrows(NullPointerException.class, () -> mBuilder.mockitoFramework(null));
    }

    @Test
    public void testMocksInitialized() throws Throwable {
        mBuilder.build().apply(mStatement, mDescription).evaluate();

        assertWithMessage("@Mock object").that(mClassUnderTest.mMock).isNotNull();
    }

    @Test
    public void testStrictness_lenientByDefault() throws Throwable {
        applyRuleOnTestThatDoesntUseExpectation(/* strictness= */ null);
    }

    @Test
    public void testStrictness_lenient() throws Throwable {
        applyRuleOnTestThatDoesntUseExpectation(Strictness.LENIENT);
    }

    @Test
    public void testStrictness_warn() throws Throwable {
        applyRuleOnTestThatDoesntUseExpectation(Strictness.WARN);
    }

    @Test
    public void testStrictness_strict() throws Throwable {
        assertThrows(UnnecessaryStubbingException.class,
                () -> applyRuleOnTestThatDoesntUseExpectation(Strictness.STRICT_STUBS));
    }

    @Test
    public void testMocksStatic() throws Throwable {
        mBuilder.mockStatic(StaticClass.class).build().apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                doReturn("mocko()").when(() -> StaticClass.marco());

                assertWithMessage("StaticClass.marco()")
                        .that(StaticClass.marco()).isEqualTo("mocko()");
                assertWithMessage("StaticClass.water()")
                        .that(StaticClass.water()).isNull(); // not mocked
            }
        }, mDescription).evaluate();
    }

    @Test
    public void testMockStatic_sameClass() throws Throwable {
        mBuilder.mockStatic(StaticClass.class);

        assertThrows(IllegalStateException.class, () -> mBuilder.mockStatic(StaticClass.class));
    }

    @Test
    public void testMocksStatic_multipleClasses() throws Throwable {
        mBuilder.mockStatic(StaticClass.class).mockStatic(AnotherStaticClass.class).build().apply(
                new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        doReturn("mocko()").when(() -> StaticClass.marco());
                        doReturn("MOCKO()").when(() -> AnotherStaticClass.marco());

                        assertWithMessage("StaticClass.marco()")
                                .that(StaticClass.marco()).isEqualTo("mocko()");
                        assertWithMessage("StaticClass.water()")
                                .that(StaticClass.water()).isNull(); // not mocked

                        assertWithMessage("AnotherStaticClass.marco()")
                                .that(AnotherStaticClass.marco()).isEqualTo("MOCKO()");
                        assertWithMessage("AnotherStaticClass.water()")
                                .that(AnotherStaticClass.water()).isNull(); // not mocked
                    }
                }, mDescription).evaluate();
    }

    @Test
    public void testSpyStatic() throws Throwable {
        mBuilder.spyStatic(StaticClass.class).build().apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                doReturn("mocko()").when(() -> StaticClass.marco());

                assertWithMessage("StaticClass.marco()")
                        .that(StaticClass.marco()).isEqualTo("mocko()");
                assertWithMessage("StaticClass.water()")
                        .that(StaticClass.water()).isEqualTo("polo");
            }
        }, mDescription).evaluate();
    }

    @Test
    public void testSpyStatic_sameClass() throws Throwable {
        mBuilder.spyStatic(StaticClass.class);

        assertThrows(IllegalStateException.class, () -> mBuilder.spyStatic(StaticClass.class));
    }

    @Test
    public void testSpyStatic_multipleClasses() throws Throwable {
        mBuilder.spyStatic(StaticClass.class).spyStatic(AnotherStaticClass.class).build()
                .apply(new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        doReturn("mocko()").when(() -> StaticClass.marco());
                        doReturn("MOCKO()").when(() -> AnotherStaticClass.marco());

                        assertWithMessage("StaticClass.marco()")
                                .that(StaticClass.marco()).isEqualTo("mocko()");
                        assertWithMessage("StaticClass.water()")
                                .that(StaticClass.water()).isEqualTo("polo");

                        assertWithMessage("AnotherStaticClass.marco()")
                                .that(AnotherStaticClass.marco()).isEqualTo("MOCKO()");
                        assertWithMessage("AnotherStaticClass.water()")
                                .that(AnotherStaticClass.water()).isEqualTo("POLO");
                    }
                }, mDescription).evaluate();
    }

    @Test
    public void testMockAndSpyStatic() throws Throwable {
        mBuilder.mockStatic(StaticClass.class).spyStatic(AnotherStaticClass.class).build()
                .apply(new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        doReturn("mocko()").when(() -> StaticClass.marco());
                        doReturn("MOCKO()").when(() -> AnotherStaticClass.marco());

                        assertWithMessage("StaticClass.marco()")
                                .that(StaticClass.marco()).isEqualTo("mocko()");
                        assertWithMessage("StaticClass.water()")
                                .that(StaticClass.water()).isNull(); // not mocked

                        assertWithMessage("AnotherStaticClass.marco()")
                                .that(AnotherStaticClass.marco()).isEqualTo("MOCKO()");
                        assertWithMessage("AnotherStaticClass.water()")
                                .that(AnotherStaticClass.water()).isEqualTo("POLO");
                    }
                }, mDescription).evaluate();
    }

    @Test
    public void testMockAndSpyStatic_sameClass() throws Throwable {
        mBuilder.mockStatic(StaticClass.class);

        assertThrows(IllegalStateException.class, () -> mBuilder.spyStatic(StaticClass.class));
    }

    @Test
    public void testSpyAndMockStatic_sameClass() throws Throwable {
        mBuilder.spyStatic(StaticClass.class);

        assertThrows(IllegalStateException.class, () -> mBuilder.mockStatic(StaticClass.class));
    }

    @Test
    public void testSpyStatic_afterConfigureSessionBuilder() throws Throwable {
        assertThrows(IllegalStateException.class,
                () -> mBuilder.configureSessionBuilder(mConfigurator).spyStatic(StaticClass.class));
    }

    @Test
    public void testMockStatic_afterConfigureSessionBuilder() throws Throwable {
        assertThrows(IllegalStateException.class, () -> mBuilder
                .configureSessionBuilder(mConfigurator).mockStatic(StaticClass.class));
    }

    @Test
    public void testConfigureSessionBuilder_afterMockStatic() throws Throwable {
        assertThrows(IllegalStateException.class, () -> mBuilder.mockStatic(StaticClass.class)
                .configureSessionBuilder(mConfigurator));
    }

    @Test
    public void testConfigureSessionBuilder_afterSpyStatic() throws Throwable {
        assertThrows(IllegalStateException.class, () -> mBuilder.spyStatic(StaticClass.class)
                .configureSessionBuilder(mConfigurator));
    }

    @Test
    public void testConfigureSessionBuilder() throws Throwable {
        mBuilder.configureSessionBuilder(mConfigurator)
                .build().apply(mStatement, mDescription).evaluate();

        assertWithMessage("visited session builder").that(mConfigurator.sessionBuilder).isNotNull();
    }

    @Test
    public void testAfterSessionFinishedCallback() throws Throwable {
        // Better not use a mock to avoid "crossed stream" issues
        AtomicBoolean called = new AtomicBoolean();
        Runnable runnable = () -> called.set(true);

        mBuilder.afterSessionFinished(runnable).build().apply(mStatement, mDescription).evaluate();

        assertWithMessage("runnable called").that(called.get()).isTrue();
    }

    @Test
    public void testMockitoFrameworkCleared() throws Throwable {
        // Better not use a mock to avoid "crossed stream" issues
        MyMockitoFramework mockitoFramework = new MyMockitoFramework();

        mBuilder.mockitoFramework(mockitoFramework).build().apply(mStatement, mDescription)
                .evaluate();

        assertWithMessage("mockito framework cleared").that(mockitoFramework.called).isTrue();
    }

    @Test
    public void testMockitoFrameworkClearedWhenTestFails() throws Throwable {
        // Better not use a mock to avoid "crossed stream" issues
        MyMockitoFramework mockitoFramework = new MyMockitoFramework();
        Exception exception = new Exception("D'OH!");

        Exception thrown = assertThrows(Exception.class,
                () -> mBuilder.mockitoFramework(mockitoFramework).build().apply(new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        throw exception;
                    }
                }, mDescription).evaluate());

        assertWithMessage("exception").that(thrown).isSameInstanceAs(exception);
        assertWithMessage("mockito framework cleared").that(mockitoFramework.called).isTrue();
    }

    private void applyRuleOnTestThatDoesntUseExpectation(@Nullable Strictness strictness)
            throws Throwable {
        Log.d(TAG, "applyRuleOnTestThatDoesntUseExpectation(): strictness= " + strictness);
        if (strictness != null) {
            mBuilder.setStrictness(strictness);
        }
        mBuilder.build().apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                when(mClassUnderTest.mMock.getMeaningOfLife()).thenReturn(42);
            }
        }, mDescription).evaluate();
    }

    private static final class ClassUnderTest {
        @Mock
        public DumbObject mMock;
    }

    private static final class StaticClass {

        public static String marco() {
            Log.d(TAG, "StaticClass.marco() called");
            return "polo";
        }

        public static String water() {
            Log.d(TAG, "StaticClass.water() called");
            return "polo";
        }
    }

    private static final class AnotherStaticClass {

        public static String marco() {
            Log.d(TAG, "AnotherStaticClass.marco() called");
            return "POLO";
        }

        public static String water() {
            Log.d(TAG, "AnotherStaticClass.water() called");
            return "POLO";
        }
    }

    private static class DumbObject {

        public int getMeaningOfLife() {
            Log.d(TAG, "getMeaningOfLife() called");
            throw new UnsupportedOperationException("Ask Bard");
        }
    }

    private static final class MySessionBuilderVisitor
            implements ExtendedMockitoRule.SessionBuilderVisitor {
        public StaticMockitoSessionBuilder sessionBuilder;

        @Override
        public void visit(StaticMockitoSessionBuilder builder) {
            sessionBuilder = builder;
        }
    }

    private static final class MyMockitoFramework implements MockitoFramework {

        public boolean called;

        @Override
        public MockitoFramework removeListener(MockitoListener arg0) {
            return null;
        }

        @Override
        public MockitoPlugins getPlugins() {
            return null;
        }

        @Override
        public InvocationFactory getInvocationFactory() {
            return null;
        }

        @Override
        public void clearInlineMocks() {
            called = true;
        }

        @Override
        public void clearInlineMock(Object arg0) {
        }

        @Override
        public MockitoFramework addListener(MockitoListener arg0) {
            return null;
        }
    };

}
