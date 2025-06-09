package uk.ac.ox.softeng.mauro.plugin.datatype

import jakarta.inject.Singleton

@Singleton
class PostgresDataTypeProviderService implements DataTypePlugin{

    String name = "PostgresDataTypeProviderService"

    String description = "PostgreSQL 9 DataTypes"

    String version = "7.3.0-SNAPSHOT"

    @Override
    List<DefaultDataType> getDataTypes() {
        [
                [label: 'bigint', description: 'signed eight-byte integer'],
                [label: 'bigserial', description: 'autoincrementing eight-byte integer'],
                [label: 'bit [ [n) ]', description: 'fixed-length bit string'],
                [label: 'bit varying [ [n) ]', description: 'variable-length bit string'],
                [label: 'boolean', description: 'logical Boolean [true/false)'],
                [label: 'box', description: 'rectangular box on a plane'],
                [label: 'bytea', description: 'binary data ["byte array")'],
                [label: 'character', description: 'fixed-length character string'],
                [label: 'character [ [n) ]', description: 'fixed-length character string'],
                [label: 'character varying', description: 'variable-length character string'],
                [label: 'character varying [ [n) ]', description: 'variable-length character string'],
                [label: 'cidr', description: 'IPv4 or IPv6 network address'],
                [label: 'circle', description: 'circle on a plane'],
                [label: 'date', description: 'calendar date [year, month, day)'],
                [label: 'double precision', description: 'double precision floating-point number [8 bytes)'],
                [label: 'inet', description: 'IPv4 or IPv6 host address'],
                [label: 'integer', description: 'signed four-byte integer'],
                [label: 'interval [ fields ] [ [p) ]', description: 'time span'],
                [label: 'json', description: 'textual JSON data'],
                [label: 'jsonb', description: 'binary JSON data, decomposed'],
                [label: 'line', description: 'infinite line on a plane'],
                [label: 'lseg', description: 'line segment on a plane'],
                [label: 'macaddr', description: 'MAC [Media Access Control) address'],
                [label: 'money', description: 'currency amount'],
                [label: 'numeric', description: 'exact numeric of selectable precision'],
                [label: 'numeric [ [p, s) ]', description: 'exact numeric of selectable precision'],
                [label: 'path', description: 'geometric path on a plane'],
                [label: 'pg_lsn', description: 'PostgreSQL Log Sequence Number'],
                [label: 'point', description: 'geometric point on a plane'],
                [label: 'polygon', description: 'closed geometric path on a plane'],
                [label: 'real', description: 'single precision floating-point number [4 bytes)'],
                [label: 'smallint', description: 'signed two-byte integer'],
                [label: 'smallserial', description: 'autoincrementing two-byte integer'],
                [label: 'serial', description: 'autoincrementing four-byte integer'],
                [label: 'text', description: 'variable-length character string'],
                [label: 'time without time zone', description: 'time of day [no time zone)'],
                [label: 'time with time zone', description: 'time of day, including time zone'],
                [label: 'timestamp without time zone', description: 'date and time [no time zone)'],
                [label: 'timestamp with time zone', description: 'date and time, including time zone'],
                [label: 'time [ [p) ] [ without time zone ]', description: 'time of day [no time zone)'],
                [label: 'time [ [p) ] with time zone', description: 'time of day, including time zone'],
                [label: 'timestamp [ [p) ] [ without time zone ]', description: 'date and time [no time zone)'],
                [label: 'timestamp [ [p) ] with time zone', description: 'date and time, including time zone'],
                [label: 'tsquery', description: 'text search query'],
                [label: 'tsvector', description: 'text search document'],
                [label: 'txid_snapshot', description: 'user-level transaction ID snapshot'],
                [label: 'uuid', description: 'universally unique identifier'],
                [label: 'xml', description: 'XML data']
        ].collect {Map<String, String> properties -> new DefaultDataType(properties)}
    }
}