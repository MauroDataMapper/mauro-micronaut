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

}
