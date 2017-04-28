package org.qamatic.mintleaf;

import org.junit.Test;
import org.qamatic.mintleaf.core.*;
import org.qamatic.mintleaf.tools.BinaryFileImportReader;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by QAmatic Team on 4/11/17.
 */
public class BinaryImportTest extends H2TestCase {


    private final ColumnMetaDataCollection cityRecordMetaData = new ColumnMetaDataCollection("CITIES") {
        {
            add(new Column("Id", 4, Types.INTEGER));
            add(new Column("City", 16, Types.CHAR));
            add(new Column("State", 2, Types.CHAR));
            add(new Column("Country", 12, Types.CHAR));
        }
    };

    private final ObjectRowListWrapper<CityRecord> cityRecords = new ObjectRowListWrapper<CityRecord>(cityRecordMetaData) {
        {
            add(new CityRecord(1, "West Chester", "PA", "USA"));
            add(new CityRecord(2, "Cherry Hill", "NJ", "USA"));
            add(new CityRecord(3, "New York", "NY", "USA"));
        }
    };

    @Test
    public void recordFileReader() throws MintleafException, URISyntaxException {
        try (BinaryReader reader = new RecordFileReader(getTestFile(), 34)) {
            int i = 0;
            for (byte[] record : reader.recordAt(1)) {
                String actual = new String(record, Charset.forName("Cp1047"));
                assertEquals(cityRecords.get(i).toString(), actual);
                i++;
            }
        }
    }

    @Test
    public void recordFileReaderStartAt() throws MintleafException, URISyntaxException {
        try (BinaryReader reader = new RecordFileReader(getTestFile(), 34)) {

            String actual = getText(reader.recordAt(3).iterator().next());
            assertEquals(cityRecords.get(2).toString(), actual);

            for (byte[] record : reader) {
                assertEquals(cityRecords.get(2).toString(), getText(record));
            }

            actual = getText(reader.recordAt(1).iterator().next());
            assertEquals(cityRecords.get(0).toString(), actual);


        }
    }

    @Test
    public void recordFileReaderNavigation() throws MintleafException, URISyntaxException {
        try (BinaryReader reader = new RecordFileReader(getTestFile())) {
            assertEquals(0, reader.getCurrentPos());
            reader.recordSize(34).recordAt(2);
            assertEquals(cityRecords.get(1).toString(), getText(reader.iterator().next()));
            assertEquals(68, reader.getCurrentPos());

            reader.reset(34);
            assertEquals(cityRecords.get(1).toString(), getText(reader.iterator().next()));
            assertEquals(68, reader.getCurrentPos());

            int i = 1, j = 0;
            for (byte[] record : reader) {
                assertEquals(cityRecords.get(i++).toString(), getText(record));
                j++;
            }
            assertEquals(2, j);

            reader.reset();
            assertEquals(0, reader.getCurrentPos());


            reader.reset(34).recordSize(17).recordAt(1);
            assertEquals("2***Cherry*Hill**", getText(reader.iterator().next()));
            assertEquals("2***Cherry*Hill**", getText(reader.iterator().next()));

            Iterator<byte[]> iterator = reader.iterator();
            assertEquals("2***Cherry*Hill**", getText(iterator.next()));
            assertEquals("***NJUSA*********", getText(iterator.next()));
            iterator = reader.recordAt(1).iterator();
            assertEquals("2***Cherry*Hill**", getText(iterator.next()));

            iterator = reader.recordAt(4).iterator();
            assertEquals("***NYUSA*********", getText(iterator.next()));

            i = 0;
            for (byte[] record : reader) {
                assertEquals("***NYUSA*********", getText(record));
                i++;
            }
            assertEquals(1, i);
        }
    }

    private String getText(byte[] record) {
        return new String(record, Charset.forName("Cp1047"));
    }

    @Test
    public void readBinaryByteReader() throws MintleafException, URISyntaxException {
        BinaryFileImportReader reader = new BinaryFileImportReader(new RecordFileReader(getTestFile(), 34), Charset.forName("Cp1047")) {

            @Override
            public Row createRowInstance(Object... params) {
                return new InMemoryRow(cityRecordMetaData);
            }
        };
        final int[] i = new int[]{0};
        reader.read(new MintleafReadListener() {
            @Override
            public Object eachRow(int rowNum, Row row) throws MintleafException {
//                String actual = new String((byte[]) row.getValue(Row.INTERNAL_OBJECT_VALUE), Charset.forName("Cp1047"));
//                assertEquals(cityRecords.get(rowNum).toString(), actual);
                return null;
            }

            @Override
            public boolean canContinue(Row row) {
                i[0]++;
                return true;
            }
        });
        assertEquals(3, i[0]);
    }


    @Test
    public void importBinaryFileTest() throws SQLException, IOException, MintleafException, URISyntaxException {
        ChangeSets.migrate(testDb.getNewConnection(), "res:/binary-import-changesets.sql", "create schema");
        BinaryReader reader = new RecordFileReader(getTestFile(), 34);

        Executable action = new Mintleaf.AnyDataToDbDataTransferBuilder().
                withImportFlavour(new BinaryFileImportReader(reader, Charset.forName("Cp1047")) {
                    @Override
                    public Row createRowInstance(Object... params) {
                        return new CityRecord(cityRecordMetaData);
                    }
                }).
                withTargetDb(testDb).
                withTargetSqlTemplate("INSERT INTO BINARY_IMPDB.CITIES VALUES($ID$, '$CITY$', '$STATE$', '$COUNTRY$')").
                build();

        action.execute();

        testDbQueries.query("SELECT * FROM BINARY_IMPDB.CITIES", (row, resultSet) -> {
            String columnName = cityRecordMetaData.getColumnName(2);
            assertEquals(cityRecords.get(row).asString(columnName), resultSet.asString(columnName));
            return null;
        });

    }

    @Test
    public void binaryFileToListTest() throws SQLException, IOException, MintleafException, URISyntaxException {

        RowListWrapper<InMemoryRow> list = new ObjectRowListWrapper<>(cityRecordMetaData);
        try (BinaryReader reader = new RecordFileReader(getTestFile(), 34).recordAt(2)) {

            reader.iterate(Charset.forName("Cp1047"), new MintleafReadListener() {
                @Override
                public Object eachRow(int rowNum, Row row) throws MintleafException {
                    list.add((InMemoryRow) row);
                    return row;
                }

                @Override
                public Row createRowInstance(Object... params) {
                    return new InMemoryRow(cityRecordMetaData);
                }
            });
        }

        assertEquals(2, list.size());
        // assertEquals(3, list.getRow(1).getId());
        assertEquals("NJ", list.getRow(0).asString("STATE"));

    }

    @Test
    public void importFromExcelToObjectListTest() throws MintleafException, URISyntaxException {

        ChangeSets.migrate(testDb.getNewConnection(), "res:/binary-import-changesets.sql", "create schema");
        BinaryReader reader = new RecordFileReader(getTestFile(), 34);

        Executable action = new Mintleaf.AnyDataToListTransferBuilder<>().
                withSource(new BinaryFileImportReader(reader, Charset.forName("Cp1047")) {
                    @Override
                    public Row createRowInstance(Object... params) {
                        return new CityRecord(cityRecordMetaData);
                    }
                }).
                build();

        action.execute();
        RowListWrapper<CityRecord> rows = (RowListWrapper<CityRecord>) action.execute();
        assertEquals(3, rows.size());
        assertEquals("CherryHill", rows.getRow(1).getValue(1));
        assertEquals("CherryHill", rows.getRow(1).getValue("City"));
    }

    private RowListWrapper<CityRecord> getRecords() throws SQLException, IOException, MintleafException, URISyntaxException {
        RowListWrapper<CityRecord> list = new ObjectRowListWrapper<CityRecord>(cityRecordMetaData);
        try (BinaryReader reader = new RecordFileReader(getTestFile(), 34)) {
            for (byte[] record : reader.recordAt(2)) {

                list.add(new CityRecord(cityRecordMetaData) {
                    {
                        setValues(record, Charset.forName("Cp1047"));
                    }
                });

            }

        }
        return list;
    }

    private File getTestFile() throws URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getClass().getResource("/impexpfiles/cp1047-1.txt");
        return new File(url.toURI());
    }


    @Test
    public void writeBinaryfile() throws MintleafException, IOException {
        Writer out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("target/cp1047-1.txt"), "Cp1047"));
        try {
            StringBuilder sb = new StringBuilder();
            for (Row r : cityRecords) {
                sb.append(r.toString());
            }
            out.write(sb.toString());
        } finally {
            out.close();
        }
        assertEquals("1***West*Chester****PAUSA*********", cityRecords.get(0).toString());

    }


}
