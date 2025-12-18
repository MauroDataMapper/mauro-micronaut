package org.maurodata.domain.datamodel

import groovy.transform.CompileStatic

@CompileStatic
enum DataModelType {
    DATA_ASSET('Data Asset'),
    DATA_STANDARD('Data Standard')

    String label

    DataModelType(String name) {
        this.label = name
    }

    String toString() {
        label
    }

    static List<String> labels() {
        values().collect {it.label}
    }
}