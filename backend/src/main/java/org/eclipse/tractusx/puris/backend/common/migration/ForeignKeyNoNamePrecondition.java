/*
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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

import liquibase.Scope;
import liquibase.database.Database;
import liquibase.exception.CustomPreconditionErrorException;
import liquibase.exception.CustomPreconditionFailedException;
import liquibase.precondition.CustomPrecondition;
import lombok.Getter;
import lombok.Setter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Precondition for Liquibase to validate if a foreign key exists without checking the foreign key name.
 * <p>
 * In case of jpa, foreign keys may have random names for older versions. Thus, the precondition is needed.
 * </p>
 */
@Getter
@Setter
public class ForeignKeyNoNamePrecondition implements CustomPrecondition {

    private String tableName;
    private String columnName;
    private String foreignTableName;

    /**
     * Checks whether at least one table constraint exists with tableName, columnName, foreignTableName
     * <p>
     * Note: if connection would be closed in a finally, then liquibase would not have a connection anymore
     *
     * @param database liquibase runs on
     * @throws CustomPreconditionFailedException if no table constraint exists
     * @throws CustomPreconditionErrorException  if an error occured during query
     */
    @Override
    public void check(Database database) throws CustomPreconditionFailedException, CustomPreconditionErrorException {

        try {
            Connection connection = database.getConnection().getUnderlyingConnection();

            String sql = " SELECT COUNT(*) AS foreign_key_exists " +
                "FROM information_schema.table_constraints AS tc " +
                "JOIN information_schema.key_column_usage AS kcu " +
                "ON tc.constraint_name = kcu.constraint_name " +
                "JOIN information_schema.constraint_column_usage AS ccu " +
                "ON ccu.constraint_name = tc.constraint_name " +
                "WHERE tc.constraint_type = 'FOREIGN KEY' " +
                "AND tc.table_name = '" + tableName + "' " +
                "AND kcu.column_name = '" + columnName + "' " +
                "AND ccu.table_name = '" + foreignTableName + "';";


            // Execute the query
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);

            if (resultSet.next()) {
                int foreignKeyExists = resultSet.getInt("foreign_key_exists");

                if (foreignKeyExists > 0) {
                    // at least one foreign key = precondition has been met
                    Scope.getCurrentScope().getLog(getClass()).info("ForeignKeyNoName precondition identified a matching constraint with tableName: " + tableName +
                        ", columnName: " + columnName + ", foreignTableName: " + foreignTableName);
                } else {
                    // Log that the foreign key does not exist
                    Scope.getCurrentScope().getLog(getClass()).info("ForeignKeyNoName precondition identified NO matching constraint with tableName: " + tableName +
                        ", columnName: " + columnName + ", foreignTableName: " + foreignTableName);
                    throw new CustomPreconditionFailedException("No foreign key exists on table " + tableName +
                        " for column " + columnName + " referencing table " + foreignTableName);
                }
            } else {
                Scope.getCurrentScope().getLog(getClass()).warning("ResultSet is empty for foreign key with tableName: " + tableName +
                    ", columnName: " + columnName + ", foreignTableName: " + foreignTableName);
                throw new CustomPreconditionErrorException("No foreign key information found for tableName " + tableName +
                    ", columnName " + columnName + ", foreignTableName: " + foreignTableName);
            }
        } catch (SQLException e) {
            Scope.getCurrentScope().getLog(getClass()).warning("Exception during SQL in ForeignKeyNoNamePrecondition for foreign key with tableName: " + tableName +
                ", columnName: " + columnName + ", foreignTableName: " + foreignTableName + " error: " + e.getMessage());
            throw new CustomPreconditionErrorException("SQL Error while checking foreign key existence: " + e.getMessage(), e);
        }
    }
}
