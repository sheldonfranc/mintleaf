/*
 *
 *   *
 *   *  *
 *   *  *   ~
 *   *  *   ~ The MIT License (MIT)
 *   *  *   ~
 *   *  *   ~ Copyright (c) 2010-2017 QAMatic Team
 *   *  *   ~
 *   *  *   ~ Permission is hereby granted, free of charge, to any person obtaining a copy
 *   *  *   ~ of this software and associated documentation files (the "Software"), to deal
 *   *  *   ~ in the Software without restriction, including without limitation the rights
 *   *  *   ~ to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   *  *   ~ copies of the Software, and to permit persons to whom the Software is
 *   *  *   ~ furnished to do so, subject to the following conditions:
 *   *  *   ~
 *   *  *   ~ The above copyright notice and this permission notice shall be included in all
 *   *  *   ~ copies or substantial portions of the Software.
 *   *  *   ~
 *   *  *   ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   *  *   ~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   *  *   ~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   *  *   ~ AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   *  *   ~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   *  *   ~ OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *   *  *   ~ SOFTWARE.
 *   *  *   ~
 *   *  *   ~
 *   *  *
 *   *
 *   *
 *
 * /
 */

package org.qamatic.mintleaf.core;

import org.qamatic.mintleaf.*;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Created by qamatic on 2/20/16.
 */
public class ExecuteQuery implements Executable<int[]> {

    private static final MintleafLogger logger = MintleafLogger.getLogger(ExecuteQuery.class);
    private ConnectionContext connectionContext;
    private ExecutionResultListener executionResultListener;

    private String sql;
    private ParameterBinding parameterBinding;
    private List<String> batchSqls; // ugly.. so need deferred execution but come back later

    public ExecuteQuery(ConnectionContext connectionContext, String sql, ParameterBinding parameterBinding) {
        this.connectionContext = connectionContext;
        this.parameterBinding = parameterBinding;
        this.sql = sql;
    }

    public ExecuteQuery(ConnectionContext connectionContext, List<String> batchSqls) {
        this.connectionContext = connectionContext;
        this.batchSqls = batchSqls;
    }

    @Override
    public int[] execute() throws MintleafException {

        if (batchSqls != null) {
            try (Statement statement = connectionContext.getConnection().createStatement()) {
                BindingParameterSets parameterSets = new BindingParameterSets(statement);
                for (String sqlItem : batchSqls) {
                    statement.addBatch(sqlItem);
                }
                int[] result = statement.executeBatch();
                if (this.executionResultListener != null) {
                    this.executionResultListener.onAfterExecuteSql(parameterSets);
                }
                return result;
            } catch (SQLException e) {
                logger.error(e);
                throw new MintleafException(e);
            }
        }

        try (PreparedStatement preparedStatement = connectionContext.getConnection().prepareStatement(this.sql)) {
            BindingParameterSets parameterSets = new BindingParameterSets(preparedStatement);
            if (parameterBinding != null) {
                parameterBinding.bindParameters(parameterSets);
            }
            if (parameterSets.isBatch()) {
                return preparedStatement.executeBatch();

            }
            int[] result = new int[]{preparedStatement.execute() ? 1 : 0};
            if (this.executionResultListener != null) {
                this.executionResultListener.onAfterExecuteSql(parameterSets);
            }
            return result;

        } catch (MintleafException e) {
            logger.error("error fetching data", e);
            throw new MintleafException(e);
        } catch (SQLException e) {
            logger.error(e);
            throw new MintleafException(e);
        }
    }


    public void setExecutionResultListener(ExecutionResultListener executionResultListener) {
        this.executionResultListener = executionResultListener;
    }
}
