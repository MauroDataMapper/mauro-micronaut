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

}
