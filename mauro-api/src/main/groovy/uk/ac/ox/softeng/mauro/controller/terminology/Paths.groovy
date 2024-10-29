package uk.ac.ox.softeng.mauro.controller.terminology

class Paths {
    public static final String CODE_SETS = "/codeSets"
    public static final String CODE_SET_BY_ID = "/codeSets/{id}"
    public static final String TERM_TO_CODE_SET = "/codeSets/{id}/terms/{termId}"
    public static final String TERMS_IN_CODE_SET = "/codeSets/{id}/terms"
    public static final String CODE_SETS_BY_FOLDER_ID = "/folders/{folderId}/codeSets"
    public static final String FINALISE_CODE_SETS = "/codeSets/{id}/finalise"
    public static final String CODE_SET_NEW_BRANCH_MODEL_VERSION = "/codeSets/{id}/newBranchModelVersion"
    public static final String DATA_FLOW_ROUTE = '/dataModels/{dataModelId}/dataFlows'
    public static final String ID_ROUTE = '/{id}'
    public static final String SOURCE_DATA_CLASS_ROUTE = '/{id}/source/{dataClassId}'
    public static final String TARGET_DATA_CLASS_ROUTE = '/{id}/target/{dataClassId}'
    public static final String TYPE_QUERY = 'type'
    public static final String DATA_CLASS_COMPONENTS_ROUTE = '/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents'
    public static final String DATA_ELEMENT_COMPONENT_ROUTE = '/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents/{dataClassComponentId}/dataElementComponents'
    public static final String SOURCE_DATA_ELEMENT_ROUTE = '/{id}/source/{dataElementId}'
    public static final String TARGET_DATA_ELEMENT_ROUTE = '/{id}/target/{dataElementId}'
    public static final String CLASSIFICATION_SCHEMES_ROUTE = '/classificationSchemes'
    public static final String CLASSIFICATION_SCHEMES_ID_ROUTE = '/classificationSchemes/{id}'
    public static final String FOLDER_CLASSIFICATION_SCHEMES_ROUTE = '/folders/{folderId}/classificationSchemes'
    public static final String CLASSIFICATION_SCHEMES_BRANCH_MODEL_VERSION = '/classificationSchemes/{id}/newBranchModelVersion'
    public static final String CLASSIFICATION_SCHEMES_DIFF = '/classificationSchemes/{id}/diff/{otherId}'
    public static final String CHILD_CLASSIFIERS_ROUTE = '/classificationSchemes/{classificationSchemeId}/classifiers/{parentClassifierId}/classifiers'
    public static final String CHILD_CLASSIFIERS_ID_ROUTE = '/classificationSchemes/{classificationSchemeId}/classifiers/{parentClassifierId}/classifiers/{childClassifierId}'
    public static final String CLASSIFIERS_ROUTE = '/classificationSchemes/{classificationSchemeId}/classifiers'
    public static final String CLASSIFIERS_ROUTE_ID = '/classificationSchemes/{classificationSchemeId}/classifiers/{id}'
    public static final String ADMINISTERED_ITEM_CLASSIFIER_ID_ROUTE = '/{administeredItemDomainType}/{administeredItemId}/classifiers/{id}'
    public static final String ADMINISTERED_ITEM_CLASSIFIER_ROUTE = '/{administeredItemDomainType}/{administeredItemId}/classifiers'

}
