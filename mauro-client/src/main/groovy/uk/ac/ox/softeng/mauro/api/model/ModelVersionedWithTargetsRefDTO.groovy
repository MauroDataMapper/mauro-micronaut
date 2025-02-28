package uk.ac.ox.softeng.mauro.api.model

class ModelVersionedWithTargetsRefDTO extends ModelVersionedRefDTO{

    boolean isNewBranchModelVersion=false
    boolean isNewDocumentationVersion=false
    boolean isNewFork=false

    List<VersionLinkTargetDTO> targets = []


    /*

   https://cdw.oxnet.nhs.uk/mauro-data-mapper/api/dataModels/d0f9adf9-9deb-4a22-aba4-12be09891377/modelVersionTree

   Object { id: "af71bcf5-ef10-4511-8702-cbba37451617", label: "Model Version Tree DataModel", modelVersion: "1.0.0", … }
id	"af71bcf5-ef10-4511-8702-cbba37451617"
label	"Model Version Tree DataModel"
branch	null
modelVersion	"1.0.0"
documentationVersion	"1.0.0"
isNewBranchModelVersion	false
isNewDocumentationVersion	false
isNewFork	false
targets	[ {…}, {…}, {…} ]
0	Object { id: "af6625cd-9def-4af8-8c1c-7c0ff8f8f0bb", description: "New Fork Of" }
id
description
1	Object { id: "3551e2f9-2c71-49f8-885c-8470c3a2b960", description: "New Model Version Of" }
2	Object { id: "ebe75b57-5c52-42ed-a981-242a4994d9ae", description: "New Model Version Of" }
    */



}
