package org.maurodata.test.domain.facet

import spock.lang.Specification
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.facet.SummaryMetadataType
import org.maurodata.test.domain.TestModelData

class SummaryMetadataSpec extends Specification {


    void 'clone -should clone new summary metadata instance '() {
        given:
        SummaryMetadata original = TestModelData.testSummaryMetadata
        original.summaryMetadataReports = [
                new SummaryMetadata().tap {
                    id = UUID.randomUUID()
                    label = 'test label'
                    description = 'test description'
                    summaryMetadataType = SummaryMetadataType.STRING
                },
                new SummaryMetadata().tap {
                    id = UUID.randomUUID()
                    label = 'test label'
                    description = 'test description'
                    summaryMetadataType = SummaryMetadataType.MAP
                } ]

        when:
        SummaryMetadata cloned = original.clone()
        then:

        //assert clone works as per groovy docs
        !cloned.is(original)
        !cloned.summaryMetadataReports.is(original.summaryMetadataReports)
        cloned.id.is(original.id)
        cloned.label.is(original.label)
        cloned.description.is(original.description)
        cloned.summaryMetadataType.is(original.summaryMetadataType)
    }

    void 'deep clone -should clone new summary metadata instance '() {
        given:
        SummaryMetadata original = TestModelData.testSummaryMetadata
        original.summaryMetadataReports = [
            new SummaryMetadata().tap {
                id = UUID.randomUUID()
                label = 'test label'
                description = 'test description'
                summaryMetadataType = SummaryMetadataType.STRING
            },
            new SummaryMetadata().tap {
                id = UUID.randomUUID()
                label = 'test label'
                description = 'test description'
                summaryMetadataType = SummaryMetadataType.MAP
            } ]

        when:
        SummaryMetadata cloned = (SummaryMetadata) original.deepClone()
        then:

        //assert clone works as per groovy docs
        !cloned.is(original)
        !cloned.summaryMetadataReports.is(original.summaryMetadataReports)
        cloned.id.is(original.id)
        cloned.label.is(original.label)
        cloned.description.is(original.description)
        cloned.summaryMetadataType.is(original.summaryMetadataType)
    }

}