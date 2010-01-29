/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.tradefed.result;

import com.android.ddmlib.testrunner.TestIdentifier;

import org.easymock.EasyMock;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestListener;

/**
 * Unit tests for {@link TestResultForwarder}.
 */
public class TestResultForwarderTest extends TestCase {

    private static final String TEST_NAME = "testName";
    private static final String CLASS_NAME = "className";
    private TestListener mJUnitListener;
    private TestResultForwarder mTestForwarder;
    private TestIdentifier mTestIdentifier;

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        super.setUp();
        mJUnitListener = EasyMock.createMock(TestListener.class);
        mTestForwarder = new TestResultForwarder(mJUnitListener);
        mTestIdentifier = new TestIdentifier(CLASS_NAME, TEST_NAME);
    }

    /**
     * Simple test for {@link TestResultForwarder#testEnded(TestIdentifier)}.
     * <p/>
     * Verifies that data put into TestIdentifier is forwarded in correct format
     */
    public void testTestEnded() {
        mJUnitListener.endTest((Test) EasyMock.anyObject());
        EasyMock.replay(mJUnitListener);
        mTestForwarder.testEnded(mTestIdentifier);
        // TODO: check format
    }

    // TODO: add more tests
}
