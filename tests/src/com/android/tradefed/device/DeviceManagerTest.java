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
package com.android.tradefed.device;

import com.android.ddmlib.IDevice;

import org.easymock.EasyMock;

import junit.framework.TestCase;

/**
 * Unit tests for {@link DeviceManager}.
 */
public class DeviceManagerTest extends TestCase {

    private static final String DEVICE_SERIAL = "1";

    private DeviceManager mDeviceManager;
    private IAndroidDebugBridge mMockAdbBridge;

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        super.setUp();
        mMockAdbBridge = EasyMock.createMock(IAndroidDebugBridge.class);
        mDeviceManager = new DeviceManager() {
            @Override
            void initAdb() {
                // do nothing
            }

            @Override
            IAndroidDebugBridge createAdbBridge() {
                return mMockAdbBridge;
            }
        };
    }

    /**
     * Test method for normal case {@link DeviceManager#allocateDevice()}.
     */
    public void testAllocateDevice() throws DeviceNotAvailableException {
        doTestAllocateDevice();
    }

    /**
     * Perform a normal case {@link DeviceManager#allocateDevice()} test scenario.
     */
    private ITestDevice doTestAllocateDevice() throws DeviceNotAvailableException {
        IDevice mockDevice = EasyMock.createNiceMock(IDevice.class);
        EasyMock.expect(mockDevice.getSerialNumber()).andReturn(DEVICE_SERIAL);
        EasyMock.expect(mMockAdbBridge.getDevices()).andReturn(new IDevice[] {mockDevice});
        EasyMock.replay(mockDevice);
        EasyMock.replay(mMockAdbBridge);
        ITestDevice testDevice = mDeviceManager.allocateDevice();
        assertEquals(mockDevice, testDevice.getIDevice());
        return testDevice;
    }

    /**
     * Test method for {@link DeviceManager#allocateDevice()} that checks if device has been
     * previously allocated, it will wait for new one.
     */
    public void testAllocateDevice_wait() throws DeviceNotAvailableException {
        // first allocate a device on separate thread
        // TODO: implement this
        //doTestAllocateDevice();
    }

    /**
     * Test method for {@link DeviceManager#freeDevice(com.android.tradefed.device.ITestDevice)}.
     */
    public void testFreeDevice() throws DeviceNotAvailableException {
        ITestDevice testDevice = doTestAllocateDevice();
        mDeviceManager.freeDevice(testDevice);
        // verify same device can be allocated again
        EasyMock.reset(mMockAdbBridge);
        doTestAllocateDevice();
    }

    /**
     * Verified that {@link DeviceManager#freeDevice(ITestDevice)} ignores a call with a device
     * that has not been allocated.
     */
    public void testFreeDevice_noop() throws DeviceNotAvailableException {
        IDevice mockIDevice = EasyMock.createMock(IDevice.class);
        ITestDevice testDevice = EasyMock.createMock(ITestDevice.class);
        EasyMock.expect(testDevice.getIDevice()).andReturn(mockIDevice);
        EasyMock.expect(mockIDevice.getSerialNumber()).andReturn("dontexist");
        EasyMock.replay(testDevice);
        EasyMock.replay(mockIDevice);
        mDeviceManager.freeDevice(testDevice);
    }

    /**
     * Test method for {@link DeviceManager#registerListener(DeviceListener)}.
     */
    public void testRegisterListener() {
        // TODO: implement this
    }

    /**
     * Test method for {@link DeviceManager#removeListener(DeviceListener)}.
     */
    public void testRemoveListener() {
        // TODO: implement this
    }

}
