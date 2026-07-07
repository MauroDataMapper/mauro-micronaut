package org.maurodata.plugin.chat.api

interface Paths {

    /*
    * ChatCapabilitiesApi
    */
    String CHAT_CAPABILITIES = '/api/chat/capabilities'

    /*
    * ChatMcpApi
    */
    String CHAT_MCP_SERVERS = '/api/chat/mcp/servers'
    String CHAT_MCP_SERVER = '/api/chat/mcp/servers/{serverId}'

    /*
    * ChatProviderKeysApi
    */
    String CHAT_PROVIDER_KEYS = '/api/chat/keys/providers'
    String CHAT_PROVIDER_KEYS_PROVIDER = '/api/chat/keys/providers/{provider}'

    /*
    * ChatSessionsApi
    */
    String CHAT_SESSIONS = '/api/chat/sessions'
    String CHAT_SESSIONS_ID = '/api/chat/sessions/{sessionId}'
    String CHAT_SESSIONS_UPDATE = '/api/chat/sessions/{sessionId}'
    String CHAT_SESSIONS_MESSAGES = '/api/chat/sessions/{sessionId}/messages'
    String CHAT_SESSIONS_MESSAGES_LIST = '/api/chat/sessions/{sessionId}/messages/list'

    /*
    * SemanticSearchApi
    */
    String SEARCH_SEMANTIC_GET = '/api/catalogueItems/semanticSearch{?requestDTO*}'
    String SEARCH_SEMANTIC_POST = '/api/catalogueItems/semanticSearch'
    String SEARCH_REBUILD_SEMANTIC_INDEXES = '/api/semanticSearchIndex/rebuild'
    String SEMANTIC_CORPORA = '/api/semanticSearchIndex/corpora'
    String SEMANTIC_INDEXES = '/api/semanticSearchIndex/indexes'
    String SEMANTIC_INDEX = '/api/semanticSearchIndex/{indexName}'
    String SEMANTIC_INDEX_EMBEDDINGS = '/api/semanticSearchIndex/{indexName}/embeddings'
    String SEMANTIC_INDEX_CORPUS_CHUNKS = '/api/semanticSearchIndex/corpora/{corpusName}/chunks'
    String SEMANTIC_INDEX_PROFILES = '/api/semanticSearchIndex/profiles'
    String SEMANTIC_INDEX_PROFILE = '/api/semanticSearchIndex/profiles/{profileName}'
    String SEMANTIC_INDEX_PROFILE_ENABLE = '/api/semanticSearchIndex/profiles/{profileName}:enable'
    String SEMANTIC_INDEX_PROFILE_DISABLE = '/api/semanticSearchIndex/profiles/{profileName}:disable'
    String SEMANTIC_INDEX_PROFILE_LINK = '/api/semanticSearchIndex/{indexName}/profiles/{profileName}:link'
    String SEMANTIC_INDEX_PROFILE_UNLINK = '/api/semanticSearchIndex/{indexName}/profiles/{profileName}:unlink'
    String SEMANTIC_INDEX_EMBEDDING_MODEL_PULL = '/api/semanticSearchIndex/embeddingModels:pull'
    String SEMANTIC_INDEXING_STATUS = '/api/semanticSearchIndex/indexing'
    String SEMANTIC_INDEXING_ENABLE = '/api/semanticSearchIndex/indexing:enable'
    String SEMANTIC_INDEXING_DISABLE = '/api/semanticSearchIndex/indexing:disable'
    String SEMANTIC_INDEXING_AUTO_RECONCILE_ENABLE = '/api/semanticSearchIndex/indexing/autoReconcile:enable'
    String SEMANTIC_INDEXING_AUTO_RECONCILE_DISABLE = '/api/semanticSearchIndex/indexing/autoReconcile:disable'
    String SEMANTIC_MODEL_INDEXES = '/api/semanticSearchIndex/modelIndexes'
    String SEMANTIC_MODEL_INDEX_STATS = '/api/semanticSearchIndex/modelIndexes/{modelId}/stats'
    String SEMANTIC_MODEL_INDEX_DEFAULT_PROFILE = '/api/semanticSearchIndex/modelIndexes/{modelId}'
    String SEMANTIC_MODEL_INDEX = '/api/semanticSearchIndex/modelIndexes/{modelId}/profiles/{profileName}'
    String SEMANTIC_MODEL_INDEX_START_DEFAULT_PROFILE = '/api/semanticSearchIndex/modelIndexes/{modelId}:start'
    String SEMANTIC_MODEL_INDEX_START = '/api/semanticSearchIndex/modelIndexes/{modelId}/profiles/{profileName}:start'
    String SEMANTIC_INDEX_JOBS = '/api/semanticSearchIndex/jobs'
    String SEMANTIC_INDEX_JOB = '/api/semanticSearchIndex/jobs/{jobId}'
    String SEMANTIC_INDEX_JOB_CANCEL = '/api/semanticSearchIndex/jobs/{jobId}:cancel'
    String SEMANTIC_INDEX_JOB_RESUME = '/api/semanticSearchIndex/jobs/{jobId}:resume'
    String SEMANTIC_INDEX_JOB_EVENTS = '/api/semanticSearchIndex/jobs/{jobId}/events'

}
