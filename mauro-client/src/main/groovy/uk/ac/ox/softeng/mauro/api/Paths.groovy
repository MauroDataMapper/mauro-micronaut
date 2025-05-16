package uk.ac.ox.softeng.mauro.api

import groovy.transform.CompileStatic

@CompileStatic
interface Paths {

    /*
    * AdminApi
    */
    String ADMIN_MODULES_LIST = '/admin/modules'
    String ADMIN_IMPORTERS_LIST = '/admin/providers/importers'
    String ADMIN_EXPORTERS_LIST = '/admin/providers/exporters'
    String ADMIN_EMAILERS_LIST = '/admin/providers/emailers'
    String ADMIN_EMAIL_SEND_TEST = '/admin/email/sendTestEmail'
    String ADMIN_EMAIL_TEST_CONNECTION = '/admin/email/testConnection'
    String ADMIN_EMAILS = '/admin/emails'
    String ADMIN_EMAIL_RETRY = '/admin/emails/{emailId}/retry'


    /*
    * ClassificationSchemeApi
    */
    String CLASSIFICATION_SCHEMES_LIST = '/classificationSchemes'
    String CLASSIFICATION_SCHEMES_ID_ROUTE = '/classificationSchemes/{id}'
    String FOLDER_CLASSIFICATION_SCHEMES_ROUTE = '/folders/{folderId}/classificationSchemes'
    String CLASSIFICATION_SCHEMES_BRANCH_MODEL_VERSION = '/classificationSchemes/{id}/newBranchModelVersion'
    String CLASSIFICATION_SCHEMES_EXPORT = '/classificationSchemes/{id}/export{/namespace}{/name}{/version}'
    String CLASSIFICATION_SCHEMES_IMPORT = '/classificationSchemes/import/{namespace}/{name}{/version}'
    String CLASSIFICATION_SCHEMES_DIFF = '/classificationSchemes/{id}/diff/{otherId}'

    /*
    * ClassificationSchemeApi
    */
    String CLASSIFIERS_ROUTE_ID = '/classificationSchemes/{classificationSchemeId}/classifiers/{id}'
    String CLASSIFIERS_ROUTE = '/classificationSchemes/{classificationSchemeId}/classifiers'
    String CHILD_CLASSIFIERS_ROUTE = '/classificationSchemes/{classificationSchemeId}/classifiers/{parentClassifierId}/classifiers'
    String CHILD_CLASSIFIERS_ID_ROUTE = '/classificationSchemes/{classificationSchemeId}/classifiers/{parentClassifierId}/classifiers/{childClassifierId}'
    String ADMINISTERED_ITEM_CLASSIFIER_ID_ROUTE = '/{administeredItemDomainType}/{administeredItemId}/classifiers/{id}'
    String ADMINISTERED_ITEM_CLASSIFIER_ROUTE = '/{administeredItemDomainType}/{administeredItemId}/classifiers'


    /*
    * ApiPropertyApi
     */
    String API_PROPERTY_LIST_PUBLIC = '/properties'
    String API_PROPERTY_LIST_ALL = '/admin/properties'
    String API_PROPERTY_SHOW = '/admin/properties/{id}'

    /*
    * SessionApi
    */
    String SESSION_IS_AUTHENTICATED = '/isAuthenticated'
    String SESSION_IS_APP_ADMIN = '/isApplicationAdministration'
    String SESSION_AUTH_DETAILS = '/authenticationDetails'
    String SESSION_CHECK_AUTHENTICATED = '/checkAuthenticated'
    String SESSION_CHECK_ANONYMOUS = '/checkAnonymous'

    /*
    * DataClassComponentApi
    */
    String DATA_FLOW_CLASS_COMPONENT_ID = '/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents/{id}'
    String DATA_FLOW_CLASS_COMPONENT_LIST = '/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents'
    String DATA_FLOW_CLASS_COMPONENT_SOURCE_CLASS = '/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents/{id}/source/{dataClassId}'
    String DATA_FLOW_CLASS_COMPONENT_TARGET_CLASS = '/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents/{id}/target/{dataClassId}'

    /*
    * DataElementComponentApi
    */
    String DATA_FLOW_ELEMENT_COMPONENT_ID = '/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents/{dataClassComponentId}/dataElementComponents/{id}'
    String DATA_FLOW_ELEMENT_COMPONENT_LIST = '/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents/{dataClassComponentId}/dataElementComponents'
    String DATA_FLOW_ELEMENT_COMPONENT_SOURCE_ELEMENT = '/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents/{dataClassComponentId}/dataElementComponents/{id}/source/{dataElementId}'
    String DATA_FLOW_ELEMENT_COMPONENT_TARGET_ELEMENT = '/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents/{dataClassComponentId}/dataElementComponents/{id}/target/{dataElementId}'

    /*
    * DataFlowApi
    */
    String DATA_FLOW_LIST = '/dataModels/{dataModelId}/dataFlows/'
    String DATA_FLOW_ID = '/dataModels/{dataModelId}/dataFlows/{id}'

    /*
    * DataClassApi
    */
    String DATA_CLASS_LIST = '/dataModels/{dataModelId}/dataClasses'
    String DATA_CLASS_ID = '/dataModels/{dataModelId}/dataClasses/{id}'
    String DATA_CLASS_CHILD_DATA_CLASS_LIST = '/dataModels/{dataModelId}/dataClasses/{parentDataClassId}/dataClasses'
    String DATA_CLASS_CHILD_DATA_CLASS_ID = '/dataModels/{dataModelId}/dataClasses/{parentDataClassId}/dataClasses/{id}'
    String DATA_CLASS_EXTENDS = '/dataModels/{dataModelId}/dataClasses/{id}/extends/{otherModelId}/{otherClassId}'

    /*
    * DataElementApi
    */
    String DATA_ELEMENT_LIST = '/dataModels/{dataModelId}/dataClasses/{dataClassId}/dataElements'
    String DATA_ELEMENT_ID = '/dataModels/{dataModelId}/dataClasses/{dataClassId}/dataElements/{id}'

    /*
    * DataModelApi
    */
    String DATA_MODEL_ROUTE = '/dataModels'
    String DATA_MODEL_ID_ROUTE = '/dataModels/{id}'
    String DATA_MODEL_ID_FINALISE = '/dataModels/{id}/finalise'
    String FOLDER_LIST_DATA_MODEL = '/folders/{folderId}/dataModels'
    String DATA_MODEL_BRANCH_MODEL_VERSION = '/dataModels/{id}/newBranchModelVersion'
    String DATA_MODEL_EXPORT = '/dataModels/{id}/export{/namespace}{/name}{/version}'
    String DATA_MODEL_IMPORT = '/dataModels/import/{namespace}/{name}{/version}'
    String DATA_MODEL_DIFF = '/dataModels/{id}/diff/{otherId}'
    String DATA_MODEL_SEARCH_GET = '/dataModels/{id}/search{?requestDTO*}'
    String DATA_MODEL_SEARCH_POST = '/dataModels/{id}/search'
    String DATA_MODEL_EXPORTERS = '/dataModels/providers/importers'
    String DATA_MODEL_SUBSET = '/dataModels/{id}/subset/{otherId}'
    String DATA_MODEL_INTERSECTS_MANY = '/dataModels/{id}/intersectsMany'

    /*
    * DataTypeApi
    */
    String DATA_TYPE_LIST = '/dataModels/{dataModelId}/dataTypes'
    String DATA_TYPE_ID = '/dataModels/{dataModelId}/dataTypes/{id}'

    /*
    * EnumerationValueApi
    */
    String ENUMERATION_VALUE_LIST = '/dataModels/{dataModelId}/dataTypes/{enumerationTypeId}/enumerationValues'
    String ENUMERATION_VALUE_ID = '/dataModels/{dataModelId}/dataTypes/{enumerationTypeId}/enumerationValues/{id}'

    /*
    * AnnotationApi
    */
    String ANNOTATION_LIST = '/{domainType}/{domainId}/annotations'
    String ANNOTATION_ID = '/{domainType}/{domainId}/annotations/{id}'
    String ANNOTATION_CHILD_LIST = '/{domainType}/{domainId}/annotations/{annotationId}/annotations'
    String ANNOTATION_CHILD_ID = '/{domainType}/{domainId}/annotations/{annotationId}/annotations/{id}'

    /*
    * MetadataApi
    */
    String METADATA_LIST = '/{domainType}/{domainId}/metadata'
    String METADATA_ID = '/{domainType}/{domainId}/metadata/{id}'

    /*
    * EditApi
    */
    String EDIT_LIST = '/{domainType}/{domainId}/edits'
    String EDIT_ID = '/{domainType}/{domainId}/edits/{id}'

    /*
    * EditApi
    */
    String SEMANTIC_LINKS_LIST = '/{domainType}/{domainId}/semanticLinks'
    String SEMANTIC_LINKS_ID = '/{domainType}/{domainId}/semanticLinks/{id}'

    /*
    * RuleApi
    */
    String RULE_LIST = '/{domainType}/{domainId}/rules'
    String RULE_ID = '/{domainType}/{domainId}/rules/{id}'

    /*
    * RuleRepresentationApi
    */
    String RULE_REPRESENTATIONS_LIST = '/{domainType}/{domainId}/rules/{ruleId}/representations'
    String RULE_REPRESENTATIONS_ID = '/{domainType}/{domainId}/rules/{ruleId}/representations/{id}'

    /*
    * ReferenceFileApi
    */
    String REFERENCE_FILE_LIST = '/{domainType}/{domainId}/referenceFiles'
    String REFERENCE_FILE_ID = '/{domainType}/{domainId}/referenceFiles/{id}'

    /*
    * SummaryMetadataApi
    */
    String SUMMARY_METADATA_LIST = '/{domainType}/{domainId}/summaryMetadata'
    String SUMMARY_METADATA_ID = '/{domainType}/{domainId}/summaryMetadata/{id}'

    /*
    * SummaryMetadataReportsApi
    */
    String SUMMARY_METADATA_REPORTS_LIST = '/{domainType}/{domainId}/summaryMetadata/{summaryMetadataId}/summaryMetadataReports'
    String SUMMARY_METADATA_REPORTS_ID = '/{domainType}/{domainId}/summaryMetadata/{summaryMetadataId}/summaryMetadataReports/{id}'

    /*
    * FolderApi
    */
    String FOLDER_LIST = '/folders'
    String FOLDER_ID = '/folders/{id}'

    String CHILD_FOLDER_LIST = '/folders/{parentId}/folders'
    String CHILD_FOLDER_ID = '/folders/{parentId}/folders/{id}'

    String FOLDER_MOVE = '/folders/{id}/folder/{destination}'
    String FOLDER_EXPORT = '/folders/{id}/export{/namespace}{/name}{/version}'
    String FOLDER_IMPORT = '/folders/import/{namespace}/{name}{/version}'

    /*
    * ImporterApi
    */
    String IMPORTER_PARAMS = '/importer/parameters/{namespace}/{name}/{version}'

    /*
    * ProfileApi
    */
    String PROFILE_DYNAMIC_PROVIDERS = '/profiles/providers/dynamic'
    String PROFILE_PROVIDERS = '/profiles/providers'
    String PROFILE_SEARCH = '/profiles/{namespace}/{name}/search'
    String PROFILE_SEARCH_ITEM = '/{domainType}/{domainId}/profiles/{namespace}/{name}/search'
    String PROFILE_DETAILS = '/profiles/providers/{namespace}/{name}/{version}'
    String PROFILE_USED = '/{domainType}/{domainId}/profiles/used'
    String PROFILE_UNUSED = '/{domainType}/{domainId}/profiles/unused'
    String PROFILE_OTHER_METADATA = '/{domainType}/{domainId}/profiles/otherMetadata'
    String PROFILE_ITEM = '/{domainType}/{domainId}/profile/{namespace}/{name}{/version}'
    String PROFILE_ITEM_VALIDATE = '/{domainType}/{domainId}/profile/{namespace}/{name}/{version}/validate'
    String PROFILE_NAMESPACES = '/metadata/namespaces{/prefix}'

    /*
    * SearchApi
    */
    String SEARCH_GET = '/search{?requestDTO*}'
    String SEARCH_POST = '/search'

    /*
    * OpenidProviderApi
     */
    String OPENID_PROVIDER_LIST = '/openidConnectProviders'

    /*
    * CatalogueUserApi
    */
    String USER_ADMIN_REGISTER = '/admin/catalogueUsers/adminRegister'
    String USER_CURRENT_USER = '/catalogueUsers/currentUser'
    String USER_CHANGE_PASSWORD = '/catalogueUsers/currentUser/changePassword'
    String USER_ID = '/catalogueUsers/{id}'
    String USER_PREFERENCES = '/catalogueUsers/{id}/userPreferences'

    /*
    * SecurableResourceGroupRoleApi
    */

    String SECURABLE_ROLE_GROUP_ID = '/{securableResourceDomainType}/{securableResourceId}/roles/{role}/userGroups/{userGroupId}'

    /*
    * SecurableResourceGroupRoleApi
    */

    String USER_GROUP_LIST = '/userGroups'

    /*
    * CodeSetApi
    */
    String CODE_SET_LIST = '/codeSets'
    String CODE_SET_ID = '/codeSets/{id}'
    String CODE_SET_TERM_ID = '/codeSets/{id}/terms/{termId}'
    String CODE_SET_TERM_LIST = '/codeSets/{id}/terms'
    String CODE_SET_FINALISE = '/codeSets/{id}/finalise'
    String CODE_SET_NEW_BRANCH_MODEL_VERSION = '/codeSets/{id}/newBranchModelVersion'
    String FOLDER_LIST_CODE_SET = '/folders/{folderId}/codeSets'
    String CODE_SET_DIFF = '/codeSets/{id}/diff/{otherId}'

    /*
    * TerminologyApi
    */
    String TERMINOLOGY_LIST = '/terminologies'
    String TERMINOLOGY_ID = '/terminologies/{id}'
    String TERMINOLOGY_FINALISE = '/terminologies/{id}/finalise'
    String TERMINOLOGY_NEW_BRANCH_MODEL_VERSION = '/terminologies/{id}/newBranchModelVersion'
    String FOLDER_LIST_TERMINOLOGY = '/folders/{folderId}/terminologies'
    String TERMINOLOGY_DIFF = '/terminologies/{id}/diff/{otherId}'
    String TERMINOLOGY_SEARCH_GET = '/terminologies/{id}/search{?requestDTO*}'
    String TERMINOLOGY_SEARCH_POST = '/terminologies/{id}/search'
    String TERMINOLOGY_EXPORT = '/terminologies/{id}/export{/namespace}{/name}{/version}'
    String TERMINOLOGY_IMPORT = '/terminologies/import/{namespace}/{name}{/version}'

    /*
    * TermApi
    */


    /*
    String TERM = "/terminologies/{terminologyId}/terms"
    final String TERM_ID = TERM + "/{id}"
    final String TERM_LIST = TERM + "/terms"
    String TERM_TREE = TERM + "/tree{/id}"
    String TERM_CODE_SETS = TERM_ID + "/codeSets"
    */

    String TERM_ID = '/terminologies/{terminologyId}/terms/{id}'
    String TERM_LIST = '/terminologies/{terminologyId}/terms'
    String TERM_TREE = '/terminologies/{terminologyId}/terms/tree{/id}'
    String TERM_CODE_SETS = '/terminologies/{terminologyId}/terms/{id}/codeSets'

    /*
    * TermRelationshipsApi
    */
    String TERM_RELATIONSHIP_LIST = '/terminologies/{terminologyId}/termRelationships'
    String TERM_RELATIONSHIP_ID = '/terminologies/{terminologyId}/termRelationships/{id}'


    /*
    * TermRelationshipTypeApi
    */
    String TERM_RELATIONSHIP_TYPE_LIST = '/terminologies/{terminologyId}/termRelationshipTypes'
    String TERM_RELATIONSHIP_TYPE_ID = '/terminologies/{terminologyId}/termRelationshipTypes/{id}'

    /*
    * TreeApi
    */
    String TREE_FOLDER = '/tree/folders{/id}'
    String TREE_ITEM = '/tree/folders/{domainType}/{id}'

    String TYPE_QUERY = 'type'

    /*
    *   LoginApi
    */
    String LOGIN = '/authentication/login'
    String LOGOUT = '/authentication/logout'

    /*
    * ApiKeyApi
    */
    String API_KEY_LIST = '/catalogueUsers/{userId}/apiKeys'
    String API_KEY_ID = '/catalogueUsers/{userId}/apiKeys/{apiKeyId}'
    String API_KEY_ENABLE = '/catalogueUsers/{userId}/apiKeys/{apiKeyId}/enable'
    String API_KEY_DISABLE = '/catalogueUsers/{userId}/apiKeys/{apiKeyId}/disable'
    String API_KEY_REFRESH = '/catalogueUsers/{userId}/apiKeys/{apiKeyId}/refresh/{expireInDays}'

    /*
    * AuthorityApi
    */
    String AUTHORITY_LIST = '/authorities'
    String AUTHORITY_ID = '/authorities/{id}'

    /*
    * PublishApi
    */
    String PUBLISHED_MODELS = '/api/published/models'
    String PUBLISHED_MODELS_NEWER_VERSIONS  = '/api/published/models/{publishedModelId}/newerVersions'

    /*
    * PublishApi
    */
    String ADMIN_SUBSCRIBED_CATALOGUES_LIST  = '/admin/subscribedCatalogues'
    String ADMIN_SUBSCRIBED_CATALOGUES_ID  = '/admin/subscribedCatalogues/{subscribedCatalogueId}'
    String ADMIN_SUBSCRIBED_CATALOGUES_TEST_CONNECTION = '/admin/subscribedCatalogues/{subscribedCatalogueId}/testConnection'

    String SUBSCRIBED_CATALOGUES_ID  = '/subscribedCatalogues/{subscribedCatalogueId}'
    String SUBSCRIBED_CATALOGUES_TYPES = '/subscribedCatalogues/types'
    String SUBSCRIBED_CATALOGUES_AUTHENTICATION_TYPES = '/subscribedCatalogues/authenticationTypes'

    String SUBSCRIBED_CATALOGUES_PUBLISHED_MODELS = '/subscribedCatalogues/{subscribedCatalogueId}/publishedModels'
    String SUBSCRIBED_CATALOGUES_PUBLISHED_MODELS_NEWER_VERSIONS = '/subscribedCatalogues/{subscribedCatalogueId}/publishedModels/{publishedModelId}/newerVersions'
    String SUBSCRIBED_CATALOGUES_LIST  = '/subscribedCatalogues'

    String SUBSCRIBED_MODELS_LIST  = '/subscribedCatalogues/{subscribedCatalogueId}/subscribedModels'
    String SUBSCRIBED_MODELS_ID = '/subscribedCatalogues/{subscribedCatalogueId}/subscribedModels/{subscribedModelId}'


}
