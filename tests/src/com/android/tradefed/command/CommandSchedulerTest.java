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
package com.android.tradefed.command;

import com.android.tradefed.config.ConfigurationException;
import com.android.tradefed.config.IConfiguration;
import com.android.tradefed.config.IConfigurationFactory;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.DeviceSelectionOptions;
import com.android.tradefed.device.IDeviceManager;
import com.android.tradefed.device.IDeviceManager.FreeDeviceState;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.device.MockDeviceManager;
import com.android.tradefed.invoker.IRescheduler;
import com.android.tradefed.invoker.ITestInvocation;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IAnswer;

import java.lang.Thread.UncaughtExceptionHandler;

/**
 * Unit tests for {@link CommandScheduler}.
 */
public class CommandSchedulerTest extends TestCase {

    private CommandScheduler mScheduler;
    private ITestInvocation mMockInvocation;
    private MockDeviceManager mMockManager;
    private IConfigurationFactory mMockConfigFactory;
    private IConfiguration mMockConfiguration;
    private CommandOptions mCommandOptions;
    private DeviceSelectionOptions mDeviceOptions;
    private NotifyingCommandListener mCmdListener;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mMockInvocation = EasyMock.createMock(ITestInvocation.class);
        mMockManager = new MockDeviceManager(0);
        mMockConfigFactory = EasyMock.createMock(IConfigurationFactory.class);
        mMockConfiguration = EasyMock.createMock(IConfiguration.class);
        mCommandOptions = new CommandOptions();
        mDeviceOptions = new DeviceSelectionOptions();
        mCmdListener = new NotifyingCommandListener();

        mScheduler = new CommandScheduler() {
            @Override
            ITestInvocation createRunInstance() {
                return mMockInvocation;
            }

            @Override
            IDeviceManager getDeviceManager() {
                return mMockManager;
            }

            @Override
            IConfigurationFactory getConfigFactory() {
                return mMockConfigFactory;
            }

            @Override
            long getCommandPollTimeMs() {
                return 20;
            }

            @Override
            void initLogging() {
                // ignore
            }

            @Override
            void cleanUp() {
                // ignore
            }
        };
    }

    /**
     * Switch all mock objects to replay mode
     */
    private void replayMocks(Object... additionalMocks) {
        EasyMock.replay(mMockConfigFactory, mMockConfiguration, mMockInvocation);
        for (Object mock : additionalMocks) {
            EasyMock.replay(mock);
        }
    }

    /**
     * Verify all mock objects
     */
    private void verifyMocks() {
        EasyMock.verify(mMockConfigFactory, mMockConfiguration, mMockInvocation);
        assertEquals("Did not receive expected number of CommandListener.commandStarted calls",
                mCmdListener.getNumExpectedCalls(), mCmdListener.getNumCalls());
        mMockManager.assertDevicesFreed();
    }

    /**
     * Test {@link CommandScheduler#run()} when no configs have been added
     */
    public void testRun_empty() throws InterruptedException {
        mMockManager.setNumDevices(1);
        replayMocks();
        mScheduler.start();
        while (!mScheduler.isAlive()) {
            Thread.sleep(10);
        }
        mScheduler.shutdown();
        // expect run not to block
        mScheduler.join();
        verifyMocks();
    }

    /**
     * Test {@link CommandScheduler#addCommand(String[])} when help mode is specified
     */
    public void testAddConfig_configHelp() throws ConfigurationException {
        String[] args = new String[] {};
        mCommandOptions.setHelpMode(true);
        setCreateConfigExpectations(args, 1);
        // expect
        mMockConfigFactory.printHelpForConfig(EasyMock.aryEq(args), EasyMock.eq(true),
                EasyMock.eq(System.out));
        replayMocks();
        mScheduler.addCommand(args, mCmdListener);
        verifyMocks();
    }

    /**
     * Test {@link CommandScheduler#run()} when one config has been added
     */
    public void testRun_oneConfig() throws Exception {
        String[] args = new String[] {};
        mMockManager.setNumDevices(2);
        setCreateConfigExpectations(args, 1);
        mCmdListener.setExpectedCalls(1);
        setExpectedInvokeCalls(1);
        replayMocks();
        mScheduler.addCommand(args, mCmdListener);
        mScheduler.start();
        waitForCommandStartedCalls();
        mScheduler.shutdown();
        mScheduler.join();
        verifyMocks();
    }

    /**
     * Test {@link CommandScheduler#run()} when one config has been added in dry-run mode
     */
    public void testRun_dryRun() throws Exception {
        String[] args = new String[] {"--dry-run"};
        mCommandOptions.setDryRunMode(true);
        mMockManager.setNumDevices(2);
        setCreateConfigExpectations(args, 1);
        mCmdListener.setExpectedCalls(0);

        replayMocks();
        mScheduler.addCommand(args, mCmdListener);
        mScheduler.start();
        waitForCommandStartedCalls();
        mScheduler.shutdown();
        mScheduler.join();
        verifyMocks();
    }

    /**
     * Sets the number of expected
     * {@link ITestInvocation#invoke(ITestDevice, IConfiguration, IRescheduler)} calls
     *
     * @param times
     */
    private void setExpectedInvokeCalls(int times) throws DeviceNotAvailableException {
        mMockInvocation.invoke((ITestDevice)EasyMock.anyObject(),
                (IConfiguration)EasyMock.anyObject(), (IRescheduler)EasyMock.anyObject());
        EasyMock.expectLastCall().times(times);
    }

    /**
     * @throws InterruptedException
     */
    private void waitForCommandStartedCalls() throws InterruptedException {
        mCmdListener.waitForExpectedCalls(1*1000);
    }

    /**
     * Test {@link CommandScheduler#run()} when one config has been added in a loop
     */
    public void testRun_oneConfigLoop() throws Exception {
        String[] args = new String[] {};
        // track if exception occurs on scheduler thread
        UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        try {
            ExceptionTracker tracker = new ExceptionTracker();
            Thread.setDefaultUncaughtExceptionHandler(tracker);
            mMockManager.setNumDevices(1);
            // config should only be created three times
            setCreateConfigExpectations(args, 3);
            mCommandOptions.setLoopMode(true);
            mCommandOptions.setMinLoopTime(0);
            // wait for invocation to be executed twice
            mCmdListener.setExpectedCalls(2);
            setExpectedInvokeCalls(2);
            replayMocks();
            mScheduler.addCommand(args, mCmdListener);
            mScheduler.start();
            waitForCommandStartedCalls();
            mScheduler.shutdown();
            mScheduler.join();
            verifyMocks();
            assertNull("exception occurred on background thread!", tracker.mThrowable);
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(defaultHandler);
        }
    }

    class ExceptionTracker implements UncaughtExceptionHandler {

        private Throwable mThrowable = null;

        /**
         * {@inheritDoc}
         */
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            e.printStackTrace();
            mThrowable  = e;
        }
    }

    /**
     * Verify that scheduler goes into shutdown mode when a {@link FatalHostError} is thrown.
     */
    public void testRun_fatalError() throws Exception {
        mCmdListener.setExpectedCalls(1);
        mMockInvocation.invoke((ITestDevice)EasyMock.anyObject(),
                (IConfiguration)EasyMock.anyObject(), (IRescheduler)EasyMock.anyObject());
        EasyMock.expectLastCall().andThrow(new FatalHostError("error"));
        String[] args = new String[] {};
        mMockManager.setNumDevices(2);
        setCreateConfigExpectations(args, 1);
        replayMocks();
        mScheduler.addCommand(args, mCmdListener);
        mScheduler.start();
        waitForCommandStartedCalls();
        // no need to call shutdown explicitly - scheduler should shutdown by itself
        mScheduler.join();
        verifyMocks();
    }

    /**
     * Test{@link CommandScheduler#run()} when config is matched to a specific device serial number
     * <p/>
     * Adds two configs to run, and verify they both run on one device
     */
    public void testRun_configSerial() throws Exception {
        String[] args = new String[] {};
        mMockManager.setNumDevices(2);
        setCreateConfigExpectations(args, 2);
        // allocate and free a device to get its serial
        ITestDevice dev = mMockManager.allocateDevice();
        mDeviceOptions.addSerial(dev.getSerialNumber());
        mCmdListener.setExpectedCalls(1);
        setExpectedInvokeCalls(1);
        replayMocks();
        mScheduler.addCommand(args, mCmdListener);
        mScheduler.addCommand(args, mCmdListener);
        mMockManager.freeDevice(dev, FreeDeviceState.AVAILABLE);

        mScheduler.start();
        waitForCommandStartedCalls();
        mScheduler.shutdown();
        mScheduler.join();
        verifyMocks();
    }

    /**
     * Test{@link CommandScheduler#run()} when config is matched to a exclude specific device serial
     * number.
     * <p/>
     * Adds two configs to run, and verify they both run on the other device
     */
    public void testRun_configExcludeSerial() throws Exception {
        String[] args = new String[] {};
        mMockManager.setNumDevices(2);
        setCreateConfigExpectations(args, 2);
        // allocate and free a device to get its serial
        ITestDevice dev = mMockManager.allocateDevice();
        mDeviceOptions.addExcludeSerial(dev.getSerialNumber());
        ITestDevice expectedDevice = mMockManager.allocateDevice();
        NotifyingCommandListener cmdListener = new NotifyingCommandListener();
        cmdListener.setExpectedCalls(1);
        setExpectedInvokeCalls(1);
        replayMocks();
        mScheduler.addCommand(args, cmdListener);
        mScheduler.addCommand(args, cmdListener);
        mMockManager.freeDevice(dev, FreeDeviceState.AVAILABLE);
        mMockManager.freeDevice(expectedDevice, FreeDeviceState.AVAILABLE);
        mScheduler.start();
        cmdListener.waitForExpectedCalls(1000);
        mScheduler.shutdown();
        mScheduler.join();
        verifyMocks();
        assertEquals(1, cmdListener.getNumCalls());
    }

    /**
     * Test {@link CommandScheduler#run()} when one config has been rescheduled
     */
    @SuppressWarnings("unchecked")
    public void testRun_rescheduled() throws Exception {
        String[] args = new String[] {};
        mMockManager.setNumDevices(2);
        setCreateConfigExpectations(args, 1);
        final IConfiguration rescheduledConfig = EasyMock.createMock(IConfiguration.class);
        EasyMock.expect(rescheduledConfig.getCommandOptions()).andStubReturn(mCommandOptions);
        EasyMock.expect(rescheduledConfig.getDeviceRequirements()).andStubReturn(
                mDeviceOptions);

        // an ITestInvocationn#invoke response for calling reschedule
        IAnswer rescheduleAndThrowAnswer = new IAnswer() {
            @Override
            public Object answer() throws Throwable {
                IRescheduler rescheduler =  (IRescheduler) EasyMock.getCurrentArguments()[2];
                rescheduler.scheduleConfig(rescheduledConfig);
                throw new DeviceNotAvailableException("not avail");
            }
        };

        mMockInvocation.invoke((ITestDevice)EasyMock.anyObject(),
                (IConfiguration)EasyMock.anyObject(), (IRescheduler)EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(rescheduleAndThrowAnswer);

        // expect one more success call
        setExpectedInvokeCalls(1);
        mCmdListener.setExpectedCalls(2);

        replayMocks(rescheduledConfig);
        mScheduler.addCommand(args, mCmdListener);
        mScheduler.start();
        waitForCommandStartedCalls();
        mScheduler.shutdown();
        mScheduler.join();

        EasyMock.verify(mMockConfigFactory, mMockConfiguration, mMockInvocation);
        assertEquals("Did not receive expected number of CommandListener.commandStarted calls",
                mCmdListener.getNumExpectedCalls(), mCmdListener.getNumCalls());
    }

    /**
     * Test {@link CommandScheduler#shutdown()} when no devices are available.
     */
    public void testShutdown() throws Exception {
        mMockManager.setNumDevices(0);
        mScheduler.start();
        while (!mScheduler.isAlive()) {
            Thread.sleep(10);
        }
        // hack - sleep a bit more to ensure allocateDevices is called
        Thread.sleep(50);
        mScheduler.shutdown();
        mScheduler.join();
        // test will hang if not successful
    }

    /**
     * Set EasyMock expectations for a create configuration call.
     */
    private void setCreateConfigExpectations(String[] args, int times)
            throws ConfigurationException {
        EasyMock.expect(
                mMockConfigFactory.createConfigurationFromArgs(EasyMock.eq(args)))
                .andReturn(mMockConfiguration)
                .times(times);
        EasyMock.expect(mMockConfiguration.getCommandOptions()).andStubReturn(mCommandOptions);
        EasyMock.expect(mMockConfiguration.getDeviceRequirements()).andStubReturn(
                mDeviceOptions);
    }
}
