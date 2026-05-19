/*
 * Copyright (c) 2026 Volkswagen AG
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.puris.backend.common.migration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class MigrationTaskService {
    @Autowired
    private final MigrationTaskRepository migrationTaskRepository;

    /**
     * Finds the pending migration task with the highest target version.
     * If multiple tasks have the same target version, the one that was created first is returned.
     * @return the latest pending migration task, or null if there are no pending tasks
     */
    public MigrationTask getLatestPendingMigrationTask() {
        List<MigrationTask> pendingTasks = migrationTaskRepository.findByStatus(MigrationTaskStatusEnumeration.PENDING);
        pendingTasks.sort(null);
        return pendingTasks.isEmpty() ? null : pendingTasks.getLast();
    }

    /**
     * Marks the given task as in progress. This should be called when a migration task is picked up for execution.
     * @param task
     */
    public void markTaskInProgress(MigrationTask task) {
        task.setStatus(MigrationTaskStatusEnumeration.IN_PROGRESS);
        saveMigrationTask(task);
    }

    /**
     * Marks the given task as completed. This should be called when a migration task has been successfully executed.
     * @param task
     */
    public void markTaskCompleted(MigrationTask task) {
        task.setStatus(MigrationTaskStatusEnumeration.COMPLETED);
        saveMigrationTask(task);
        List<MigrationTask> pendingTasks = migrationTaskRepository.findByStatus(MigrationTaskStatusEnumeration.PENDING);
        // skip remaining tasks with lower version than the completed one
        for (MigrationTask pendingTask : pendingTasks) {
            if (pendingTask.compareTo(task) < 0) {
                pendingTask.setStatus(MigrationTaskStatusEnumeration.SKIPPED);
                saveMigrationTask(pendingTask); 
            }
        }
    }

    /**
     * Marks the given task as failed and saves the provided error messages in the logs. This should be called when a migration task has failed during execution.
     * @param task
     * @param errors
     */
    public void markTaskFailed(MigrationTask task, List<String> errors) {
        task.setStatus(MigrationTaskStatusEnumeration.FAILED);
        String logs = "";
        for (String error : errors) {
            logs += error + "\n";
        }
        task.setLogs(logs);
        saveMigrationTask(task);
    }

    /**
     * Saves the given migration task to the database.
     * @param task
     */
    private void saveMigrationTask(@NonNull MigrationTask task) {
        migrationTaskRepository.save(task);
    }
}
