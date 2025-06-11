package org.maurodata.test.domain.classifier

import spock.lang.Specification
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.diff.ObjectDiff

class ClassificationSchemeSpec extends Specification {

    void 'clone -should clone object '() {
        given:
        ClassificationScheme original = new ClassificationScheme().tap {
            id = UUID.randomUUID()
            label = 'classification scheme label'
            readableByEveryone = true
            readableByAuthenticatedUsers = true
            label = 'classification scheme label'
        }
        Classifier classifier1 = new Classifier().tap {
            classificationScheme = original
            id = UUID.randomUUID()
            label = 'classifier 1  label'
            description = 'classifier 1 description'
        }
        Classifier classifier2 = new Classifier().tap {
            classificationScheme = original
            id = UUID.randomUUID()
            label = 'classifier 2  label'
            description = 'classifier 2 description'
        }
        Classifier classifier3 = new Classifier().tap {
            classificationScheme = original
            id = UUID.randomUUID()
            label = 'child classifier classifier2  label'
            description = 'child classifier classifier2 description'
            parentClassifier = classifier2
        }
        classifier2.childClassifiers = [classifier3]
        original.classifiers = [classifier1, classifier2]

        when:
        ClassificationScheme cloned = original.clone()
        then:
        cloned
        //assert clone works as per groovy docs
        !cloned.is(original)
        !cloned.classifiers.is(original.classifiers)
        cloned.id.is(original.id)
        cloned.label.is(original.label)
        cloned.description.is(original.description)

        when:
        cloned.setAssociations()

        then:
        !cloned.is(original)
        !cloned.classifiers.is(original.classifiers)
        cloned.id.is(original.id)
        cloned.label.is(original.label)
        cloned.description.is(original.description)
        cloned.classifiers.size() == original.classifiers.size()
        List<Classifier> clonedChildClassifiers = cloned.classifiers.childClassifiers.flatten() as List<Classifier>
        clonedChildClassifiers.size() == 1
        clonedChildClassifiers[0].classificationScheme.is(cloned)
        List<Classifier> originalChildClassifiers = original.classifiers.childClassifiers.flatten() as List<Classifier>
        originalChildClassifiers.size() == 1
        originalChildClassifiers[0].classificationScheme.is(original)

        ObjectDiff objectDiff = original.diff(cloned)
        objectDiff.numberOfDiffs == 0
    }
}