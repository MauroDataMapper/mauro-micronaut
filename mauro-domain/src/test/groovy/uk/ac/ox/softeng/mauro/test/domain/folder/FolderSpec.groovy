package uk.ac.ox.softeng.mauro.test.domain.folder

import spock.lang.Specification
import uk.ac.ox.softeng.mauro.domain.folder.Folder

class FolderSpec extends Specification {
    static String LABEL =  'My Test Folder'
    static String AUTHOR =  'My Test Folder author : anon'
    static String DESCRIPTION =  'This is an example of a folder'

    static Folder testFolder = Folder.build (label: "$LABEL", author: "$AUTHOR", description: "$DESCRIPTION") {}

    def "Test the DSL for creating objects"() {

        given:
        testFolder
        when:
        testFolder != null

        then:
        testFolder.label == LABEL
        testFolder.author == AUTHOR
        testFolder.description == DESCRIPTION
    }

}
