// Copyright (c) 2017, Xiaomi, Inc.  All rights reserved.
// This source code is licensed under the Apache License Version 2.0, which
// can be found in the LICENSE file in the root directory of this source tree.
package com.xiaomi.infra.pegasus.rpc.async;

import com.xiaomi.infra.pegasus.rpc.KeyHasher;
import com.xiaomi.infra.pegasus.rpc.ReplicationException;
import com.xiaomi.infra.pegasus.base.error_code;
import com.xiaomi.infra.pegasus.base.rpc_address;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

/**
 * ClusterManager Tester.
 *
 * @author sunweijie@xiaomi.com
 * @version 1.0
 */
public class ClusterManagerTest {
    @Before
    public void before() throws Exception {
    }

    @After
    public void after() throws Exception {
    }

    private static boolean isOpenAuth() {
        // openAuth is on by default
        String prop = System.getProperties().getProperty("test.open.auth");
        return (prop != null) ? Boolean.valueOf(prop) : false;
    }

    /**
     * Method: getReplicaSession(rpc_address address)
     */
    @Test
    public void testGetReplicaSession() throws Exception {
        String[] address_list = {
                "127.0.0.1:1", "127.0.0.1:2", "127.0.0.1:3"
        };

        ClusterManager testManager;
        if (isOpenAuth()) {
            testManager = new ClusterManager.Builder(1000, 1, address_list).openAuth("xxxx", "xxxx").build();
        } else {
            testManager = new ClusterManager.Builder(1000, 1, address_list).build();
        }
        // input an invalid rpc address
        rpc_address address = new rpc_address();
        ReplicaSession session = testManager.getReplicaSession(address);
        Assert.assertNull(session);
    }

    /**
     * Method: openTable(String name, KeyHasher h)
     */
    @Test
    public void testOpenTable() throws Exception {
        // test invalid meta list
        String[] addr_list = {"127.0.0.1:123", "127.0.0.1:124", "127.0.0.1:125"};
        ClusterManager testManager;
        if (isOpenAuth()) {
            testManager = new ClusterManager.Builder(1000, 1, addr_list).openAuth("xxxx", "xxxx").build();
        } else {
            testManager = new ClusterManager.Builder(1000, 1, addr_list).build();
        }

        TableHandler result = null;
        try {
            result = testManager.openTable("testName", KeyHasher.DEFAULT);
        } catch (ReplicationException e) {
            Assert.assertEquals(error_code.error_types.ERR_SESSION_RESET, e.err_type);
        } finally {
            Assert.assertNull(result);
        }
        testManager.close();

        // test partially invalid meta list
        String[] addr_list2 = {"127.0.0.1:123", "127.0.0.1:34603", "127.0.0.1:34601", "127.0.0.1:34602"};
        if (isOpenAuth()) {
            testManager = new ClusterManager.Builder(1000, 1, addr_list2).openAuth("xxxx", "xxxx").build();
        } else {
            testManager = new ClusterManager.Builder(1000, 1, addr_list2).build();
        }
        try {
            result = testManager.openTable("hehe", KeyHasher.DEFAULT);
        } catch (ReplicationException e) {
            Assert.assertEquals(error_code.error_types.ERR_OBJECT_NOT_FOUND, e.err_type);
        } finally {
            Assert.assertNull(result);
        }

        // test open an valid table
        try {
            result = testManager.openTable("temp", KeyHasher.DEFAULT);
        } catch (ReplicationException e) {
            Assert.fail();
        } finally {
            Assert.assertNotNull(result);
            // in onebox, we create a table named temp with 8 partitions in default.
            Assert.assertEquals(8, result.getPartitionCount());
        }
        testManager.close();
    }
}
