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

package org.qamatic.mintleaf.tools;

import org.qamatic.mintleaf.*;
import org.qamatic.mintleaf.readers.CsvReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by qamatic on 3/6/16.
 */
public class CsvImporter extends SqlBatchInsertReadListener implements Executable<Boolean> {

    private static final MintleafLogger logger = MintleafLogger.getLogger(CsvImporter.class);
    private String sourceCsvFile;
    private ConnectionContext targetDb;
    private String targetSqlTemplate;


    public CsvImporter(String sourceCsvFile, ConnectionContext targetDb,
                       String targetSqlTemplate) {
        this.sourceCsvFile = sourceCsvFile;
        this.targetDb = targetDb;
        this.targetSqlTemplate = targetSqlTemplate;
    }


    @Override
    protected String getSql() {
        return this.targetSqlTemplate;
    }

    @Override
    public MintleafReader getReader() throws MintleafException {
        try {
            File f = new File(this.sourceCsvFile);
            if (!f.exists()) {
                logger.error("file not found " + sourceCsvFile);
                throw new MintleafException("file not found " + sourceCsvFile);
            }
            MintleafReader reader = new CsvReader(new FileReader(f));
            reader.setReadListener(this);
            return reader;

        } catch (IOException e) {
            throw new MintleafException(e);
        }
    }

    @Override
    protected ConnectionContext getConnectionContext() {
        return targetDb;
    }
}
