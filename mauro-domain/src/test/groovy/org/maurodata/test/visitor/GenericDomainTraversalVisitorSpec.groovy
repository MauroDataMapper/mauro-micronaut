package org.maurodata.test.visitor

import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.model.Item

import org.maurodata.visitor.CommonVisitorRegistries
import org.maurodata.visitor.GenericDomainTraversalVisitor
import org.maurodata.visitor.VisitorRegistry
import spock.lang.Specification

class GenericDomainTraversalVisitorSpec extends Specification {


    void 'generic visitor applies both exact and supertype handlers'() {
        given:
        List<String> calls = []
        GenericDomainTraversalVisitor visitor = new GenericDomainTraversalVisitor()
            .onEnter(Item) {Item item -> calls << "item:${item.class.simpleName}".toString() }
            .onEnter(DataType) {DataType dataType -> calls << "datatype:${dataType.label}".toString() }

        DataType dataType = DataType.build {
            label 'example'
        }

        when:
        visitor.visitDataType(dataType)

        then:
        calls == ['item:DataType', 'datatype:example']
    }

    void 'registries can be composed into one traversal visitor'() {
        given:
        List<String> calls = []
        VisitorRegistry regA = new VisitorRegistry()
            .onEnter(DataType) {DataType ignored -> calls << 'A' }
        VisitorRegistry regB = new VisitorRegistry()
            .onEnter(DataType) {DataType ignored -> calls << 'B' }

        GenericDomainTraversalVisitor visitor = new GenericDomainTraversalVisitor(regA + regB)
        DataType dataType = DataType.build {
            label 'combined'
        }

        when:
        visitor.visitDataType(dataType)

        then:
        calls == ['A', 'B']
    }

    void 'treeify-like bundle can be reused with generic traversal visitor'() {
        given:
        GenericDomainTraversalVisitor visitor = new GenericDomainTraversalVisitor(CommonVisitorRegistries.treeifyVisitor())
        DataType dataType = DataType.build {
            label 'Test DataType'
            referenceClass DataClass.build {
                label 'Test DataClass'
                dataElement {
                    label 'Nested DataElement'
                }
            }
        }

        when:
        visitor.visitDataType(dataType)

        then:
        dataType.referenceClass.label == 'Test DataClass'
        !dataType.referenceClass.dataElements
    }

}
