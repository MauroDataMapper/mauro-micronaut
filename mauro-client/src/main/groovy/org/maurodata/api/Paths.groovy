package org.maurodata.api

import groovy.transform.CompileStatic

@CompileStatic
interface Paths {

    /*
    * AdminApi
    */
    String ADMIN_MODULES_LIST = '/api/admin/modules'
    String ADMIN_IMPORTERS_LIST = '/api/admin/providers/importers'
    String ADMIN_EXPORTERS_LIST = '/api/admin/providers/exporters'
    String ADMIN_EMAILERS_LIST = '/api/admin/providers/emailers'
    String ADMIN_EMAIL_SEND_TEST = '/api/admin/email/sendTestEmail'
    String ADMIN_EMAIL_TEST_CONNECTION = '/api/admin/email/testConnection'
    String ADMIN_EMAILS = '/api/admin/emails'
    String ADMIN_EMAIL_RETRY = '/api/admin/emails/{emailId}/retry'


    /*
    * ClassificationSchemeApi
    */
    String CLASSIFICATION_SCHEMES_LIST = '/api/classificationSchemes'
    String CLASSIFICATION_SCHEMES_ID_ROUTE = '/api/classificationSchemes/{id}'
    String FOLDER_CLASSIFICATION_SCHEMES_ROUTE = '/api/folders/{folderId}/classificationSchemes'
    String CLASSIFICATION_SCHEMES_BRANCH_MODEL_VERSION = '/api/classificationSchemes/{id}/newBranchModelVersion'
    String CLASSIFICATION_SCHEMES_EXPORT = '/api/classificationSchemes/{id}/export{/namespace}{/name}{/version}'
    String CLASSIFICATION_SCHEMES_IMPORT = '/api/classificationSchemes/import/{namespace}/{name}{/version}'
    String CLASSIFICATION_SCHEMES_DIFF = '/api/classificationSchemes/{id}/diff/{otherId}'
    String CLASSIFICATION_SCHEMES_READ_BY_AUTHENTICATED = '/api/classificationSchemes/{id}/readByAuthenticated'
    String CLASSIFICATION_SCHEMES_READ_BY_EVERYONE = '/api/classificationSchemes/{id}/readByEveryone'
    String CLASSIFICATION_SCHEMES_PERMISSIONS = '/api/classificationSchemes/{id}/permissions'
    String CLASSIFICATION_SCHEMES_LIST_PAGED = '/api/classificationSchemes{?params*}'
    String FOLDER_CLASSIFICATION_SCHEMES_ROUTE_PAGED = '/api/folders/{folderId}/classificationSchemes{?params*}'

    /*
    * ClassificationSchemeApi
    */
    String CLASSIFIERS_ROUTE_ID = '/api/classificationSchemes/{classificationSchemeId}/classifiers/{id}'
    String CLASSIFIERS_ROUTE = '/api/classificationSchemes/{classificationSchemeId}/classifiers'
    String CHILD_CLASSIFIERS_ROUTE = '/api/classificationSchemes/{classificationSchemeId}/classifiers/{parentClassifierId}/classifiers'
    String CHILD_CLASSIFIERS_ID_ROUTE = '/api/classificationSchemes/{classificationSchemeId}/classifiers/{parentClassifierId}/classifiers/{childClassifierId}'
    String ADMINISTERED_ITEM_CLASSIFIER_ID_ROUTE = '/api/{administeredItemDomainType}/{administeredItemId}/classifiers/{id}'
    String ADMINISTERED_ITEM_CLASSIFIER_ROUTE = '/api/{administeredItemDomainType}/{administeredItemId}/classifiers'
    String CLASSIFIERS_ROUTE_PAGED = '/api/classificationSchemes/{classificationSchemeId}/classifiers{?params*}'
    String CHILD_CLASSIFIERS_ROUTE_PAGED = '/api/classificationSchemes/{classificationSchemeId}/classifiers/{parentClassifierId}/classifiers{?params*}'
    String ADMINISTERED_ITEM_CLASSIFIER_ROUTE_PAGED = '/api/{administeredItemDomainType}/{administeredItemId}/classifiers{?params*}'
    String ALL_CLASSIFIERS_ROUTE = '/api/classifiers'
    /*
    * ApiPropertyApi
     */
    String API_PROPERTY_LIST_PUBLIC = '/api/properties'
    String API_PROPERTY_LIST_ALL = '/api/admin/properties'
    String API_PROPERTY_SHOW = '/api/admin/properties/{id}'
    String API_PROPERTY_LIST_PUBLIC_PAGED = '/api/properties{?params*}'
    String API_PROPERTY_LIST_ALL_PAGED = '/api/admin/properties{?params*}'

    /*
    * SessionApi
    */
    String SESSION_IS_AUTHENTICATED = '/api/session/isAuthenticated'
    String SESSION_IS_APP_ADMIN = '/api/session/isApplicationAdministration'
    String SESSION_AUTH_DETAILS = '/api/session/authenticationDetails'
    String SESSION_CHECK_AUTHENTICATED = '/api/session/checkAuthenticated'
    String SESSION_CHECK_ANONYMOUS = '/api/session/checkAnonymous'

    /*
    * DataClassComponentApi
    */
    String DATA_FLOW_CLASS_COMPONENT_ID = '/api/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents/{id}'
    String DATA_FLOW_CLASS_COMPONENT_LIST = '/api/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents'
    String DATA_FLOW_CLASS_COMPONENT_SOURCE_CLASS = '/api/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents/{id}/source/{dataClassId}'
    String DATA_FLOW_CLASS_COMPONENT_TARGET_CLASS = '/api/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents/{id}/target/{dataClassId}'
    String DATA_FLOW_CLASS_COMPONENT_LIST_PAGED = '/api/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents{?params*}'

    /*
    * DataElementComponentApi
    */
    String DATA_FLOW_ELEMENT_COMPONENT_ID = '/api/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents/{dataClassComponentId}/dataElementComponents/{id}'
    String DATA_FLOW_ELEMENT_COMPONENT_LIST = '/api/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents/{dataClassComponentId}/dataElementComponents'
    String DATA_FLOW_ELEMENT_COMPONENT_SOURCE_ELEMENT = '/api/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents/{dataClassComponentId}/dataElementComponents/{id}/source/{dataElementId}'
    String DATA_FLOW_ELEMENT_COMPONENT_TARGET_ELEMENT = '/api/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents/{dataClassComponentId}/dataElementComponents/{id}/target/{dataElementId}'
    String DATA_FLOW_ELEMENT_COMPONENT_LIST_PAGED = '/api/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents/{dataClassComponentId}/dataElementComponents{?params*}'
    /*
    * DataFlowApi
    */
    String DATA_FLOW_LIST = '/api/dataModels/{dataModelId}/dataFlows'
    String DATA_FLOW_ID = '/api/dataModels/{dataModelId}/dataFlows/{id}'
    String DATA_FLOW_LIST_PAGED = '/api/dataModels/{dataModelId}/dataFlows{?params*}'
    String DATA_FLOW_EXPORT = '/api/dataModels/{dataModelId}/dataFlows/{id}/export{/namespace}{/name}{/version}'
    String DATA_FLOW_IMPORT = '/api/dataModels/{dataModelId}/dataFlows/import{/namespace}{/name}{/version}'
    String DATA_FLOW_EXPORTERS = '/api/dataFlows/providers/exporters'
    String DATA_FLOW_IMPORTERS = '/api/dataFlows/providers/importers'
    /*
    * DataClassApi
    */
    String DATA_CLASS_LIST = '/api/dataModels/{dataModelId}/dataClasses'
    String DATA_CLASS_ID = '/api/dataModels/{dataModelId}/dataClasses/{id}'
    String DATA_CLASS_CHILD_DATA_CLASS_LIST = '/api/dataModels/{dataModelId}/dataClasses/{parentDataClassId}/dataClasses'
    String DATA_CLASS_CHILD_DATA_CLASS_ID = '/api/dataModels/{dataModelId}/dataClasses/{parentDataClassId}/dataClasses/{id}'
    String DATA_CLASS_EXTENDS = '/api/dataModels/{dataModelId}/dataClasses/{id}/extends/{otherModelId}/{otherClassId}'
    String DATA_CLASS_DOI = '/api/dataClasses/{id}/doi'
    String DATA_CLASS_SEARCH = '/api/dataModels/{dataModelId}/dataClasses{?params*}'
    String DATA_CLASS_COPY = '/api/dataModels/{dataModelId}/dataClasses/{otherModelId}/{dataClassId}'
    String ALL_DATA_CLASSES = '/api/dataModels/{dataModelId}/allDataClasses'

    /*
    * DataElementApi
    */
    String DATA_ELEMENT_LIST = '/api/dataModels/{dataModelId}/dataClasses/{dataClassId}/dataElements'
    String DATA_ELEMENT_IN_MODEL_LIST = '/api/dataModels/{dataModelId}/dataElements'
    String DATA_ELEMENT_ID = '/api/dataModels/{dataModelId}/dataClasses/{dataClassId}/dataElements/{id}'
    String DATA_ELEMENT_DOI = '/api/dataElements/{id}/doi'
    String DATA_ELEMENT_LIST_PAGED = '/api/dataModels/{dataModelId}/dataClasses/{dataClassId}/dataElements{?params*}'
    String DATA_ELEMENT_COPY = '/api/dataModels/{dataModelId}/dataClasses/dataElements/{dataClassId}/{otherModelId}/{otherDataClassId}/{dataElementId}'



    /*
    * DataModelApi
    */
    String DATA_MODEL_ROUTE = '/api/dataModels'
    String DATA_MODEL_ID_ROUTE = '/api/dataModels/{id}'
    String DATA_MODEL_ID_FINALISE = '/api/dataModels/{id}/finalise'
    String FOLDER_LIST_DATA_MODEL = '/api/folders/{folderId}/dataModels'
    String CREATE_DATA_MODEL = '/api/folders/{folderId}/dataModels{?defaultDataTypeProvider}'
    String DATA_MODEL_BRANCH_MODEL_VERSION = '/api/dataModels/{id}/newBranchModelVersion'
    String DATA_MODEL_EXPORT = '/api/dataModels/{id}/export{/namespace}{/name}{/version}'
    String DATA_MODEL_IMPORT = '/api/dataModels/import/{namespace}/{name}{/version}'
    String DATA_MODEL_DIFF = '/api/dataModels/{id}/diff/{otherId}'
    String DATA_MODEL_SEARCH_GET = '/api/dataModels/{id}/search{?requestDTO*}'
    String DATA_MODEL_SEARCH_POST = '/api/dataModels/{id}/search'
    String DATA_MODEL_EXPORTERS = '/api/dataModels/providers/exporters'
    String DATA_MODEL_IMPORTERS = '/api/dataModels/providers/importers'
    String DATA_MODEL_SUBSET = '/api/dataModels/{id}/subset/{otherId}'
    String DATA_MODEL_INTERSECTS_MANY = '/api/dataModels/{id}/intersectsMany'
    String DATA_MODEL_READ_BY_AUTHENTICATED = '/api/dataModels/{id}/readByAuthenticated'
    String DATA_MODEL_READ_BY_EVERYONE = '/api/dataModels/{id}/readByEveryone'
    String DATA_MODEL_VERSION_LINKS = '/api/dataModels/{id}/versionLinks'
    String DATA_MODEL_SIMPLE_MODEL_VERSION_TREE = '/api/dataModels/{id}/simpleModelVersionTree{?branchesOnly}'
    String DATA_MODEL_MODEL_VERSION_TREE = '/api/dataModels/{id}/modelVersionTree'
    String DATA_MODEL_CURRENT_MAIN_BRANCH = '/api/dataModels/{id}/currentMainBranch'
    String DATA_MODEL_LATEST_MODEL_VERSION = '/api/dataModels/{id}/latestModelVersion'
    String DATA_MODEL_LATEST_FINALISED_MODEL = '/api/dataModels/{id}/latestFinalisedModel'
    String DATA_MODEL_COMMON_ANCESTOR = '/api/dataModels/{id}/commonAncestor/{other_model_id}'
    String DATA_MODEL_MERGE_DIFF = '/api/dataModels/{id}/mergeDiff/{otherId}'
    String DATA_MODEL_PERMISSIONS = '/api/dataModels/{id}/permissions'
    String DATA_MODEL_DOI = '/api/dataModels/{id}/doi'
    String DATA_MODEL_DATATYPE_PROVIDERS = '/api/dataModels/providers/defaultDataTypeProviders'
    String DATA_MODEL_TYPES = '/api/dataModels/types'
    String DATA_MODEL_MERGE_INTO = '/api/dataModels/{id}/mergeInto/{otherId}'


    /*
    * DataTypeApi
    */
    String DATA_TYPE_LIST = '/api/dataModels/{dataModelId}/dataTypes'
    String DATA_TYPE_ID = '/api/dataModels/{dataModelId}/dataTypes/{id}'
    String DATA_TYPE_LIST_PAGED = '/api/dataModels/{dataModelId}/dataTypes{?params*}'

    /*
    * EnumerationValueApi
    */
    String ENUMERATION_VALUE_LIST = '/api/dataModels/{dataModelId}/dataTypes/{enumerationTypeId}/enumerationValues'
    String ENUMERATION_VALUE_ID = '/api/dataModels/{dataModelId}/dataTypes/{enumerationTypeId}/enumerationValues/{id}'
    String ENUMERATION_VALUE_LIST_PAGED = '/api/dataModels/{dataModelId}/dataTypes/{enumerationTypeId}/enumerationValues{?params*}'

    /*
    * AnnotationApi
    */
    String ANNOTATION_LIST = '/api/{domainType}/{domainId}/annotations'
    String ANNOTATION_ID = '/api/{domainType}/{domainId}/annotations/{id}'
    String ANNOTATION_CHILD_LIST = '/api/{domainType}/{domainId}/annotations/{annotationId}/annotations'
    String ANNOTATION_CHILD_ID = '/api/{domainType}/{domainId}/annotations/{annotationId}/annotations/{id}'
    String ANNOTATION_LIST_PAGED = '/api/{domainType}/{domainId}/annotations{?params*}'

    /*
    * MetadataApi
    */
    String METADATA_LIST = '/api/{domainType}/{domainId}/metadata'
    String METADATA_ID = '/api/{domainType}/{domainId}/metadata/{id}'
    String METADATA_LIST_PAGED = '/api/{domainType}/{domainId}/metadata{?params*}'

    /*
    * EditApi
    */
    String EDIT_LIST = '/api/{domainType}/{domainId}/edits'
    String EDIT_ID = '/api/{domainType}/{domainId}/edits/{id}'
    String EDIT_LIST_PAGED = '/api/{domainType}/{domainId}/edits{?params*}'

    /*
    * EditApi
    */
    String SEMANTIC_LINKS_LIST = '/api/{domainType}/{domainId}/semanticLinks'
    String SEMANTIC_LINKS_ID = '/api/{domainType}/{domainId}/semanticLinks/{id}'
    String SEMANTIC_LINKS_LIST_PAGED = '/api/{domainType}/{domainId}/semanticLinks{?params*}'

    /*
    * RuleApi
    */
    String RULE_LIST = '/api/{domainType}/{domainId}/rules'
    String RULE_ID = '/api/{domainType}/{domainId}/rules/{id}'
    String RULE_LIST_PAGED = '/api/{domainType}/{domainId}/rules{?params*}'

    /*
    * RuleRepresentationApi
    */
    String RULE_REPRESENTATIONS_LIST = '/api/{domainType}/{domainId}/rules/{ruleId}/representations'
    String RULE_REPRESENTATIONS_ID = '/api/{domainType}/{domainId}/rules/{ruleId}/representations/{id}'
    String RULE_REPRESENTATIONS_LIST_PAGED = '/api/{domainType}/{domainId}/rules/{ruleId}/representations{?params*}'

    /*
    * ReferenceFileApi
    */
    String REFERENCE_FILE_LIST = '/api/{domainType}/{domainId}/referenceFiles'
    String REFERENCE_FILE_ID = '/api/{domainType}/{domainId}/referenceFiles/{id}'
    String REFERENCE_FILE_LIST_PAGED = '/api/{domainType}/{domainId}/referenceFiles{?params*}'

    /*
    * SummaryMetadataApi
    */
    String SUMMARY_METADATA_LIST = '/api/{domainType}/{domainId}/summaryMetadata'
    String SUMMARY_METADATA_ID = '/api/{domainType}/{domainId}/summaryMetadata/{id}'
    String SUMMARY_METADATA_LIST_PAGED = '/api/{domainType}/{domainId}/summaryMetadata{?params*}'

    /*
    * SummaryMetadataReportsApi
    */
    String SUMMARY_METADATA_REPORTS_LIST = '/api/{domainType}/{domainId}/summaryMetadata/{summaryMetadataId}/summaryMetadataReports'
    String SUMMARY_METADATA_REPORTS_ID = '/api/{domainType}/{domainId}/summaryMetadata/{summaryMetadataId}/summaryMetadataReports/{id}'
    String SUMMARY_METADATA_REPORTS_LIST_PAGED = '/api/{domainType}/{domainId}/summaryMetadata/{summaryMetadataId}/summaryMetadataReports{?params*}'

    /*
    * FolderApi
    */
    String FOLDER_LIST = '/api/folders'
    String FOLDER_ID = '/api/folders/{id}'

    String CHILD_FOLDER_LIST = '/api/folders/{parentId}/folders'
    String CHILD_FOLDER_ID = '/api/folders/{parentId}/folders/{id}'

    String FOLDER_MOVE = '/api/folders/{id}/folder/{destination}'
    String FOLDER_EXPORT = '/api/folders/{id}/export{/namespace}{/name}{/version}'
    String FOLDER_IMPORT = '/api/folders/import/{namespace}/{name}{/version}'

    String FOLDER_READ_BY_AUTHENTICATED = '/api/folders/{id}/readByAuthenticated'
    String FOLDER_READ_BY_EVERYONE = '/api/folders/{id}/readByEveryone'
    String FOLDER_PERMISSIONS = '/api/folders/{id}/permissions'
    String FOLDER_DOI = '/api/folders/{id}/doi'

    String FOLDER_IMPORTERS = '/api/folders/providers/importers'
    String FOLDER_EXPORTERS = '/api/folders/providers/exporters'



    /*
    * VersionedFolderApi
     */
    String VERSIONED_FOLDER_LIST = '/api/versionedFolders'
    String VERSIONED_FOLDER_ID = '/api/versionedFolders/{id}'

    String CHILD_VERSIONED_FOLDER_LIST = '/api/versionedFolders/{parentId}/folders'
    String FOLDER_CHILD_VERSIONED_FOLDER_ID = '/api/versionedFolders/{parentId}/folders/{id}'

    String VERSIONED_FOLDER_FINALISE = '/api/versionedFolders/{id}/finalise'
    String VERSIONED_FOLDER_NEW_BRANCH_MODEL_VERSION = '/api/versionedFolders/{id}/newBranchModelVersion'

    String VERSIONED_FOLDER_READ_BY_AUTHENTICATED = '/api/versionedFolders/{id}/readByAuthenticated'
    String VERSIONED_FOLDER_READ_BY_EVERYONE = '/api/versionedFolders/{id}/readByEveryone'
    String VERSIONED_FOLDER_PERMISSIONS = '/api/versionedFolders/{id}/permissions'
    String VERSIONED_FOLDER_DOI = '/api/versionedFolders/{id}/doi'

    String VERSIONED_FOLDER_SIMPLE_MODEL_VERSION_TREE = '/api/versionedFolders/{id}/simpleModelVersionTree{?branchesOnly}'
    String VERSIONED_FOLDER_MODEL_VERSION_TREE = '/api/versionedFolders/{id}/modelVersionTree'
    String VERSIONED_FOLDER_CURRENT_MAIN_BRANCH = '/api/versionedFolders/{id}/currentMainBranch'
    String VERSIONED_FOLDER_LATEST_MODEL_VERSION = '/api/versionedFolders/{id}/latestModelVersion'
    String VERSIONED_FOLDER_LATEST_FINALISED_MODEL = '/api/versionedFolders/{id}/latestFinalisedModel'
    String VERSIONED_FOLDER_COMMON_ANCESTOR = '/api/versionedFolders/{id}/commonAncestor/{other_model_id}'
    String VERSIONED_FOLDER_MERGE_DIFF = '/api/versionedFolders/{id}/mergeDiff/{otherId}'
    String VERSIONED_FOLDER_MERGE_INTO = '/api/versionedFolders/{id}/mergeInto/{otherId}'
    String VERSIONED_FOLDER_IMPORT = '/api/versionedFolders/import/{namespace}/{name}{/version}'
    String VERSIONED_FOLDER_EXPORT ='/api/versionedFolders/{id}/export{/namespace}{/name}{/version}'
    /*
    * ImporterApi
    */
    String IMPORTER_PARAMS = '/api/importer/parameters/{namespace}/{name}/{version}'

    /*
    * ProfileApi
    */
    String PROFILE_DYNAMIC_PROVIDERS = '/api/profiles/providers/dynamic'
    String PROFILE_PROVIDERS = '/api/profiles/providers'
    String PROFILE_SEARCH = '/api/profiles/{namespace}/{name}'
    String PROFILE_SEARCH_ITEM = '/api/{domainType}/{domainId}/profiles/{namespace}/{name}/search'
    String PROFILE_DETAILS = '/api/profiles/providers/{namespace}/{name}{/version}'
    String PROFILE_USED = '/api/{domainType}/{domainId}/profiles/used'
    String PROFILE_UNUSED = '/api/{domainType}/{domainId}/profiles/unused'
    String PROFILE_OTHER_METADATA = '/api/{domainType}/{domainId}/profiles/otherMetadata'
    String PROFILE_ITEM = '/api/{domainType}/{domainId}/profile/{namespace}/{name}{/version}'
    String PROFILE_ITEM_VALIDATE = '/api/{domainType}/{domainId}/profile/{namespace}/{name}{/version}/validate'
    String PROFILE_NAMESPACES = '/api/metadata/namespaces{/prefix}'

    /*
    * SearchApi
    */
    String SEARCH_GET = '/api/search{?requestDTO*}'
    String SEARCH_POST = '/api/search'

    /*
    * OpenidProviderApi
     */
    String OPENID_PROVIDER_LIST = '/api/openidConnectProviders'

    /*
    * CatalogueUserApi
    */
    String USER_ADMIN_REGISTER = '/api/admin/catalogueUsers/adminRegister'
    String USER_CURRENT_USER = '/api/catalogueUsers/currentUser'
    String USER_CHANGE_PASSWORD = '/api/catalogueUsers/currentUser/changePassword'
    String USER_ID = '/api/catalogueUsers/{id}'
    String USER_PREFERENCES = '/api/catalogueUsers/{id}/userPreferences'
    String USER_IMAGE = '/api/catalogueUsers/{id}/image'


    /*
    * SecurableResourceGroupRoleApi
    */

    String SECURABLE_ROLE_GROUP_ID = '/api/{securableResourceDomainType}/{securableResourceId}/roles/{role}/userGroups/{userGroupId}'

    /*
    * SecurableResourceGroupRoleApi
    */

    String USER_GROUP_LIST = '/api/userGroups'

    /*
    * CodeSetApi
    */
    String CODE_SET_LIST = '/api/codeSets'
    String CODE_SET_ID = '/api/codeSets/{id}'
    String CODE_SET_TERM_ID = '/api/codeSets/{id}/terms/{termId}'
    String CODE_SET_TERM_LIST = '/api/codeSets/{id}/terms'
    String CODE_SET_FINALISE = '/api/codeSets/{id}/finalise'
    String CODE_SET_NEW_BRANCH_MODEL_VERSION = '/api/codeSets/{id}/newBranchModelVersion'
    String FOLDER_LIST_CODE_SET = '/api/folders/{folderId}/codeSets'
    String CODE_SET_DIFF = '/api/codeSets/{id}/diff/{otherId}'
    String CODE_SET_READ_BY_AUTHENTICATED = '/api/codeSets/{id}/readByAuthenticated'
    String CODE_SET_READ_BY_EVERYONE = '/api/codeSets/{id}/readByEveryone'
    String CODE_SET_FOLDER_PERMISSIONS = '/api/codeSets/{id}/permissions'
    String CODE_SET_DOI = '/api/codeSets/{id}/doi'
    String CODE_SET_LIST_PAGED = '/api/codeSets{?params*}'
    String CODE_SET_TERM_LIST_PAGED = '/api/codeSets/{id}/terms{?params*}'
    String CODE_SET_IMPORT = '/api/codeSets/import/{namespace}/{name}{/version}'
    String CODE_SET_SIMPLE_MODEL_VERSION_TREE = '/api/codeSets/{id}/simpleModelVersionTree{?branchesOnly}'


    /*
    * TerminologyApi
    */
    String TERMINOLOGY_LIST = '/api/terminologies'
    String TERMINOLOGY_ID = '/api/terminologies/{id}'
    String TERMINOLOGY_FINALISE = '/api/terminologies/{id}/finalise'
    String TERMINOLOGY_NEW_BRANCH_MODEL_VERSION = '/api/terminologies/{id}/newBranchModelVersion'
    String FOLDER_LIST_TERMINOLOGY = '/api/folders/{folderId}/terminologies'
    String TERMINOLOGY_DIFF = '/api/terminologies/{id}/diff/{otherId}'
    String TERMINOLOGY_SEARCH_GET = '/api/terminologies/{id}/search{?requestDTO*}'
    String TERMINOLOGY_SEARCH_POST = '/api/terminologies/{id}/search'
    String TERMINOLOGY_EXPORT = '/api/terminologies/{id}/export{/namespace}{/name}{/version}'
    String TERMINOLOGY_IMPORT = '/api/terminologies/import/{namespace}/{name}{/version}'
    String TERMINOLOGY_READ_BY_AUTHENTICATED = '/api/terminologies/{id}/readByAuthenticated'
    String TERMINOLOGY_READ_BY_EVERYONE = '/api/terminologies/{id}/readByEveryone'
    String TERMINOLOGY_PERMISSIONS = '/api/terminologies/{id}/permissions'
    String TERMINOLOGY_LIST_IMPORTERS = '/api/terminologies/providers/importers'
    String TERMINOLOGY_LIST_EXPORTERS = '/api/terminologies/providers/exporters'
    String TERMINOLOGY_DOI = '/api/terminologies/{id}/doi'

    String TERMINOLOGY_SIMPLE_MODEL_VERSION_TREE = '/api/terminologies/{id}/simpleModelVersionTree{?branchesOnly}'
    String TERMINOLOGY_LIST_PAGED = '/api/terminologies{?params*}'


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

    String TERM_ID = '/api/terminologies/{terminologyId}/terms/{id}'
    String TERM_LIST = '/api/terminologies/{terminologyId}/terms'
    String TERM_TREE = '/api/terminologies/{terminologyId}/terms/tree{/id}'
    String TERM_CODE_SETS = '/api/terminologies/{terminologyId}/terms/{id}/codeSets'
    String TERM_DOI = '/api/terms/{id}/doi'
    String TERM_LIST_PAGED = '/api/terminologies/{terminologyId}/terms{?params*}'
    String TERM_CODE_SETS_PAGED = '/api/terminologies/{terminologyId}/terms/{id}/codeSets{?params*}'

    /*
    * TermRelationshipsApi
    */
    String TERM_RELATIONSHIP_LIST = '/api/terminologies/{terminologyId}/termRelationships'
    String TERM_RELATIONSHIP_ID = '/api/terminologies/{terminologyId}/termRelationships/{id}'
    String TERM_RELATIONSHIP_LIST_PAGED = '/api/terminologies/{terminologyId}/termRelationships'
    String TERM_RELATIONSHIP_BY_TERM_ID_LIST = '/api/terminologies/{terminologyId}/terms/{termId}/termRelationships'
    String TERM_RELATIONSHIP_BY_TERM_ID_ID = '/api/terminologies/{terminologyId}/terms/{termId}/termRelationships/{id}'

    /*
    * TermRelationshipTypeApi
    */
    String TERM_RELATIONSHIP_TYPE_LIST = '/api/terminologies/{terminologyId}/termRelationshipTypes'
    String TERM_RELATIONSHIP_TYPE_ID = '/api/terminologies/{terminologyId}/termRelationshipTypes/{id}'
    String TERM_RELATIONSHIP_TYPE_LIST_PAGED = '/api/terminologies/{terminologyId}/termRelationshipTypes{?params*}'

    /*
    * TreeApi
    */
    String TREE_FOLDER = '/api/tree/folders{/id}'
    String TREE_ITEM = '/api/tree/folders/{domainType}/{id}'
    String TREE_ITEM_ANCESTORS = '/api/tree/folders/{domainType}/{id}/ancestors'
    String TREE_FOLDER_ANCESTORS = '/api/tree/folders/{id}/ancestors'
    String TREE_FOLDER_SEARCH = '/api/tree/folders/search/{searchTerm}'

    String TYPE_QUERY = 'type'

    /*
    *   LoginApi
    */
    String LOGIN = '/api/authentication/login'
    String LOGOUT = '/api/authentication/logout'

    /*
    * ApiKeyApi
    */
    String API_KEY_LIST = '/api/catalogueUsers/{userId}/apiKeys'
    String API_KEY_ID = '/api/catalogueUsers/{userId}/apiKeys/{apiKeyId}'
    String API_KEY_ENABLE = '/api/catalogueUsers/{userId}/apiKeys/{apiKeyId}/enable'
    String API_KEY_DISABLE = '/api/catalogueUsers/{userId}/apiKeys/{apiKeyId}/disable'
    String API_KEY_REFRESH = '/api/catalogueUsers/{userId}/apiKeys/{apiKeyId}/refresh/{expireInDays}'

    /*
    * AuthorityApi
    */
    String AUTHORITY_LIST = '/api/authorities'
    String AUTHORITY_ID = '/api/authorities/{id}'

    /*
    * PublishApi
    */
    String PUBLISHED_MODELS = '/api/api/published/models'
    String PUBLISHED_MODELS_NEWER_VERSIONS = '/api/api/published/models/{publishedModelId}/newerVersions'

    /*
    * PublishApi
    */
    String ADMIN_SUBSCRIBED_CATALOGUES_LIST = '/api/admin/subscribedCatalogues'
    String ADMIN_SUBSCRIBED_CATALOGUES_ID = '/api/admin/subscribedCatalogues/{subscribedCatalogueId}'
    String ADMIN_SUBSCRIBED_CATALOGUES_TEST_CONNECTION = '/api/admin/subscribedCatalogues/{subscribedCatalogueId}/testConnection'

    String SUBSCRIBED_CATALOGUES_ID = '/api/subscribedCatalogues/{subscribedCatalogueId}'
    String SUBSCRIBED_CATALOGUES_TYPES = '/api/subscribedCatalogues/types'
    String SUBSCRIBED_CATALOGUES_AUTHENTICATION_TYPES = '/api/subscribedCatalogues/authenticationTypes'

    String SUBSCRIBED_CATALOGUES_PUBLISHED_MODELS = '/api/subscribedCatalogues/{subscribedCatalogueId}/publishedModels'
    String SUBSCRIBED_CATALOGUES_PUBLISHED_MODELS_NEWER_VERSIONS = '/api/subscribedCatalogues/{subscribedCatalogueId}/publishedModels/{publishedModelId}/newerVersions'
    String SUBSCRIBED_CATALOGUES_LIST = '/api/subscribedCatalogues'

    String SUBSCRIBED_MODELS_LIST = '/api/subscribedCatalogues/{subscribedCatalogueId}/subscribedModels'
    String SUBSCRIBED_MODELS_ID = '/api/subscribedCatalogues/{subscribedCatalogueId}/subscribedModels/{subscribedModelId}'

    /*
    * Reference data models
    * NOTE: for now this stubbed in FolderController and
    */
    String REFERENCE_DATA_MODELS_LIST = '/api/referenceDataModels'
    String FOLDER_REFERENCE_DATA_MODELS= '/api/folders/{id}/referenceDataModels'
}
