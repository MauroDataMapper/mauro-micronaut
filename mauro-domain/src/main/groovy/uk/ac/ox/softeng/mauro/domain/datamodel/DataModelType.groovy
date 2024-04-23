package uk.ac.ox.softeng.mauro.domain.datamodel

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
}