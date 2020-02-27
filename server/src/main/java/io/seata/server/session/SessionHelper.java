/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server.session;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.server.UUIDGenerator;

/**
 * The type Session helper.
 *
 * @author sharajava
 */
public class SessionHelper {

    private SessionHelper() {}

    public static BranchSession newBranchByGlobal(GlobalSession globalSession, BranchType branchType, String resourceId, String lockKeys, String clientId) {
        return newBranchByGlobal(globalSession, branchType, resourceId, null, lockKeys, clientId);
    }

    /**
     * New branch by global branch session.
     *
     * @param globalSession the global session
     * @param branchType    the branch type
     * @param resourceId    the resource id
     * @param lockKeys      the lock keys
     * @param clientId      the client id
     * @return the branch session
     */
    public static BranchSession newBranchByGlobal(GlobalSession globalSession, BranchType branchType, String resourceId,
            String applicationData, String lockKeys, String clientId) {
        BranchSession branchSession = new BranchSession();

        branchSession.setXid(globalSession.getXid());
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(UUIDGenerator.generateUUID());
        branchSession.setBranchType(branchType);
        branchSession.setResourceId(resourceId);
        branchSession.setLockKey(lockKeys);
        branchSession.setClientId(clientId);
        branchSession.setApplicationData(applicationData);

        return branchSession;
    }

    /**
     * End committed.
     * <p>
     *   vergilyn-comment, 2020-02-25 >>>> <br/>
     *     1. UPDATE global_table (status = Committed) <br/>
     *     2. DELETE lock_table (xid, branchIds) <br/>
     *     3. DELETE global_table  <br/>
     *
     *   vergilyn-optimize, 2020-02-25 >>>> 1&3可以合并！
     * </p>
     * @param globalSession the global session
     * @throws TransactionException the transaction exception
     */
    public static void endCommitted(GlobalSession globalSession) throws TransactionException {
        globalSession.changeStatus(GlobalStatus.Committed);
        globalSession.end();
    }

    /**
     * End commit failed.
     * <p>vergilyn-comment, 2020-02-25 >>>> <br/>
     *   1. UPDATE global_table (status = CommitFailed) <br/>
     *   2. DELETE lock_table (xid, branchIds) <br/>
     *   3. DELETE global_table <br/>
     * </p>
     * @param globalSession the global session
     * @throws TransactionException the transaction exception
     */
    public static void endCommitFailed(GlobalSession globalSession) throws TransactionException {
        globalSession.changeStatus(GlobalStatus.CommitFailed);
        globalSession.end();
    }

    /**
     * End rollbacked.
     * <p>vergilyn-comment, 2020-02-25 >>>> <br/>
     *   1. UPDATE global_table, status = TimeoutRollbacked/Rollbacked <br/>
     *   2. DELETE lock_table (xid, branchIds) <br/>
     *   3. DELETE global_table <br/>
     * </p>
     * @param globalSession the global session
     * @throws TransactionException the transaction exception
     */
    public static void endRollbacked(GlobalSession globalSession) throws TransactionException {
        GlobalStatus currentStatus = globalSession.getStatus();
        if (isTimeoutGlobalStatus(currentStatus)) {
            globalSession.changeStatus(GlobalStatus.TimeoutRollbacked);
        } else {
            globalSession.changeStatus(GlobalStatus.Rollbacked);
        }
        globalSession.end();
    }

    /**
     * End rollback failed.
     * <p>vergilyn-comment, 2020-02-25 >>>> <br/>
     *   1. UPDATE global_table, status = TimeoutRollbackFailed/RollbackFailed
     *   2. DELETE lock_table (xid, branchIds)
     *   3. DELETE global_table
     * </p>
     * @param globalSession the global session
     * @throws TransactionException the transaction exception
     */
    public static void endRollbackFailed(GlobalSession globalSession) throws TransactionException {
        GlobalStatus currentStatus = globalSession.getStatus();
        if (isTimeoutGlobalStatus(currentStatus)) {
            globalSession.changeStatus(GlobalStatus.TimeoutRollbackFailed);
        } else {
            globalSession.changeStatus(GlobalStatus.RollbackFailed);
        }
        globalSession.end();
    }

    public static boolean isTimeoutGlobalStatus(GlobalStatus status) {
        return status == GlobalStatus.TimeoutRollbacked
                || status == GlobalStatus.TimeoutRollbackFailed
                || status == GlobalStatus.TimeoutRollbacking
                || status == GlobalStatus.TimeoutRollbackRetrying;
    }
}
