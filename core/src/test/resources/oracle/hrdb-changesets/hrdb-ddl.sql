-- <ChangeSet id="create countries" delimiter=";" />

CREATE TABLE COUNTRIES
(
  COUNTRY_ID   CHAR(2 BYTE) NOT NULL,
  COUNTRY_NAME VARCHAR2(40 BYTE),
  REGION_ID    NUMBER
) LOGGING;

CREATE UNIQUE INDEX COUNTRY_C_ID_PKX ON COUNTRIES
(
  COUNTRY_ID ASC
);

ALTER TABLE COUNTRIES
ADD CONSTRAINT COUNTRY_C_ID_PK PRIMARY KEY (COUNTRY_ID);

