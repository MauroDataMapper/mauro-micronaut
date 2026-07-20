package org.maurodata.service.chat

import groovy.transform.CompileStatic

@CompileStatic
interface ChatPromptAssetService {
    List<ChatPromptAssetDefinition> listAssets()
    List<ChatPromptAssetDefinition> listAssetsByType(String type)
    ChatPromptAssetDefinition findAsset(String id)
    List<ChatPromptAssetDefinition> searchAssets(String query)
}
