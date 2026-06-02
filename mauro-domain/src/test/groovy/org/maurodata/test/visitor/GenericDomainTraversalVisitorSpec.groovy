package org.maurodata.test.visitor

import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.model.Item
import org.maurodata.visitor.AbstractDomainTraversalVisitor
import org.maurodata.visitor.CommonVisitorRegistries
import org.maurodata.visitor.GenericDomainTraversalVisitor
import org.maurodata.visitor.VisitorRegistry
import spock.lang.Specification

class GenericDomainTraversalVisitorSpec extends Specification {

    void 'legacy hooks are still called via abstract visitor'() {
        given:
        TestLegacyVisitor visitor = new TestLegacyVisitor()
        DataType first = DataType.build {
            label 'first'
        }
        DataType second = DataType.build {
            label 'second'
        }

        when:
        visitor.visitDataType(first)
        visitor.visitDataType(second)

        then:
        visitor.visitedDataTypeLabels == ['first', 'second']
    }

    void 'generic visitor applies both exact and supertype handlers'() {
        given:
        List<String> calls = []
        GenericDomainTraversalVisitor visitor = new GenericDomainTraversalVisitor()
            .on(Item) {Item item -> calls << "item:${item.class.simpleName}" }
            .on(DataType) {DataType dataType -> calls << "datatype:${dataType.label}" }

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
            .on(DataType) {DataType ignored -> calls << 'A' }
        VisitorRegistry regB = new VisitorRegistry()
            .on(DataType) {DataType ignored -> calls << 'B' }

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

    private static class TestLegacyVisitor extends AbstractDomainTraversalVisitor {

        final List<String> visitedDataTypeLabels = []

        @Override
        protected void onVisitDataType(DataType dataType) {
            visitedDataTypeLabels << dataType.label
        }
    }
}
