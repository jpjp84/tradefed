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
package com.android.tradefed.invoker;

import com.android.ddmlib.Log;
import com.android.tradefed.config.ConfigurationException;
import com.android.tradefed.config.IConfiguration;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.targetsetup.IBuildInfo;
import com.android.tradefed.targetsetup.IBuildProvider;
import com.android.tradefed.targetsetup.ITargetPreparer;
import com.android.tradefed.targetsetup.TargetSetupError;
import com.android.tradefed.testtype.IDeviceTest;
import com.android.tradefed.testtype.IRemoteTest;

import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestResult;

/**
 * Default implementation of {@link ITestInvocation}.
 * <p/>
 * Loads major objects based on {@link IConfiguration}
 *   - retrieves build
 *   - prepares target
 *   - runs tests
 *   - reports results
 */
public class TestInvocation implements ITestInvocation {

    private static final String LOG_TAG = "BaseTestInvocation";

    /**
     * Constructs a {@link TestInvocation}
     */
    public TestInvocation() {
    }

    /**
     * {@inheritDoc}
     */
    public void invoke(ITestDevice device, IConfiguration config) {
        try {
            IBuildProvider buildProvider = config.getBuildProvider();
            ITargetPreparer preparer = config.getTargetPreparer();
            Test test = config.getTest();
            ITestInvocationListener listener = config.getTestInvocationListener();
            IBuildInfo info = buildProvider.getBuild();
            preparer.setUp(device, info);
            runTests(device, test, listener);
        } catch (TargetSetupError e) {
            Log.e(LOG_TAG, e);
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, e);
        } catch (ConfigurationException e) {
            Log.e(LOG_TAG, e);
        }
    }

    /**
     * Runs the test.
     *
     * @param device the {@link ITestDevice} to run tests on
     * @param test the {@link Test} to run
     * @param listener the {@link ITestInvocationListener} that listens for test results in real
     * time
     */
    private void runTests(ITestDevice device, Test test, ITestInvocationListener listener) {
        if (test instanceof IDeviceTest) {
            ((IDeviceTest)test).setDevice(device);
        }

        if (test instanceof IRemoteTest) {
            // run as a remote test, so results are forwarded directly to TestInvocationListener
            ((IRemoteTest) test).run(listener);
        } else if (listener instanceof TestListener) {
            // run as a JUnit test, and wrap the TestInvocationListener in a JUnit listener
            TestResult result = new TestResult();
            result.addListener((TestListener)listener);
            test.run(result);
        } else {
            // TODO: add a class which can forward JUnit forwarder
            throw new UnsupportedOperationException();
        }
    }
}
