package uk.ac.ox.softeng.mauro.test.domain

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.domain.facet.Rule
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.test.domain.datamodel.DataModelSpec
import uk.ac.ox.softeng.mauro.test.domain.terminology.TerminologySpec

class TestModelData {

    static final String FILE_CONTENTS_STRING = 'This is a string of words for a test file contents field in reference file'
    static Folder testFolder = new Folder().tap {
        id = UUID.randomUUID()
        label = 'folder label'
        description = 'folder description'
    }

    static List<Term> testTerms = [
            new Term().tap {
                code = 'B15.0'
                definition = 'Hepatitis A with hepatic coma'
                id = UUID.randomUUID()
                version = 1
            },

            new Term().tap {
                code = 'B15.9'
                definition = 'Hepatitis A without hepatic coma'
                description = 'Hepatitis A (acute)(viral) NOS'
                id = UUID.randomUUID()
                version = 0
            }]


    static CodeSet testCodeSet = new CodeSet().tap {
        label = 'codeset label'
        description = 'codeset description'
        author = 'a n other'
        folder = testFolder
        terms = testTerms
    }
    static DataModel dataModelTest = DataModelSpec.testDataModel

    static Terminology testTerminologyModel = TerminologySpec.testTerminology

    static Folder testChildFolder =
            new Folder().tap {
                label = 'test child folder 1 label'
                description = 'test child folder 1 description'
                parentFolder = testFolder
           }


    static Folder testComplexFolder = testFolder.tap {
        terminologies = [testTerminologyModel]
        codeSets = [testCodeSet]
        dataModels = [dataModelTest]
    }
    static Term childTerm =
            new Term().tap {
                code = "A9"
                definition = "a definition of the medical term"
                id = UUID.randomUUID()
            }

    static Terminology testSimpleTerminology =
           new Terminology().tap{
                label = "My child folder  terminology"
                author = "A N Other"
                description  = "Terminology description test inside child folder"
                id  = UUID.randomUUID()
                terms = [ childTerm ]
            }

    static ReferenceFile testReferenceFile =  new ReferenceFile().tap {
        id = UUID.randomUUID()
        fileName = 'test file name'
        fileType = 'text/plain'
        fileContents =  FILE_CONTENTS_STRING.bytes
        fileSize = fileContents.size()
    }

    static SummaryMetadata testSummaryMetadata =  new SummaryMetadata().tap {
        id = UUID.randomUUID()
        label = 'test label'
        description = 'test description'
        summaryMetadataType = SummaryMetadataType.STRING
    }

    static SummaryMetadataReport testSummaryMetadataReport =  new SummaryMetadataReport().tap {
        id = UUID.randomUUID()
        reportValue  = 'test report value'
    }

    static Rule testRule = new Rule().tap {
        id = UUID.randomUUID()
        name = 'test rule'
        description = 'test description'
    }
}


