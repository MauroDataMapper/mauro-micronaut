package org.maurodata.test.visitor

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataType
import org.maurodata.visitor.CommonVisitorRegistries
import org.maurodata.visitor.GenericDomainTraversalVisitor
import spock.lang.Specification

@MicronautTest
class TreeifySpec extends Specification {


    void "Test simple DataType treeify"() {
        when:
            DataType dataType = DataType.build {
                label "Test DataType"
                referenceClass DataClass.build {
                    label "Test DataClass"
                    dataElement {
                        label "Test DataElement"
                    }
                }
            }
        def visitor = new GenericDomainTraversalVisitor(
            CommonVisitorRegistries.treeifyVisitor()
        )

        dataType.accept(visitor)
        then:
        dataType.referenceClass.label == "Test DataClass"
        !dataType.referenceClass.dataElements
    }


}
