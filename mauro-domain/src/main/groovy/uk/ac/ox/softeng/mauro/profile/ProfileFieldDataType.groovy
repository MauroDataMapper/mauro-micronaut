package uk.ac.ox.softeng.mauro.profile

enum ProfileFieldDataType {

    BOOLEAN('boolean'),
    STRING('string'),
    TEXT('text'),
    INT('int'),
    DECIMAL('decimal'),
    DATE('date'),
    DATETIME('datetime'),
    TIME('time'),
    FOLDER('folder'),
    MODEL('model'),
    ENUMERATION('enumeration'),
    JSON('json')

    String label

    ProfileFieldDataType(String name) {
        this.label = name
    }

}