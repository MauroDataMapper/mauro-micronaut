package org.maurodata.service.chat

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@Singleton
@CompileStatic
class ChatPromptAssetRegistryService implements ChatPromptAssetService {

    private final ChatPromptAssetDefinitionLoader promptAssetDefinitionLoader

    ChatPromptAssetRegistryService(ChatPromptAssetDefinitionLoader promptAssetDefinitionLoader) {
        this.promptAssetDefinitionLoader = promptAssetDefinitionLoader
    }

    @Override
    List<ChatPromptAssetDefinition> listAssets() {
        promptAssetDefinitionLoader.listDefinitions()
    }

    @Override
    List<ChatPromptAssetDefinition> listAssetsByType(String type) {
        String wanted = type == null ? null : type.trim()
        if (wanted == null || wanted.isEmpty()) {
            return listAssets()
        }
        listAssets().findAll {ChatPromptAssetDefinition asset ->
            wanted.equalsIgnoreCase(asset.type)
        } as List<ChatPromptAssetDefinition>
    }

    @Override
    ChatPromptAssetDefinition findAsset(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null
        }
        listAssets().find {ChatPromptAssetDefinition asset -> asset.id == id}
    }

    @Override
    List<ChatPromptAssetDefinition> searchAssets(String query) {
        if (query == null || query.trim().isEmpty()) {
            return listAssets()
        }
        String lower = query.toLowerCase(Locale.ROOT)
        List<ChatPromptAssetDefinition> definitions = listAssets()
        List<ChatPromptAssetDefinition> primaryMatches = definitions.findAll {ChatPromptAssetDefinition asset ->
            asset.id.toLowerCase(Locale.ROOT).contains(lower) ||
                asset.name.toLowerCase(Locale.ROOT).contains(lower) ||
                (asset.keywords ?: [] as List<String>).any {String keyword -> keyword.toLowerCase(Locale.ROOT).contains(lower)}
        } as List<ChatPromptAssetDefinition>
        if (!primaryMatches.isEmpty()) {
            return primaryMatches
        }
        definitions.findAll {ChatPromptAssetDefinition asset ->
            containsIgnoreCase(asset.description, lower) ||
                containsIgnoreCase(asset.instruction, lower)
        } as List<ChatPromptAssetDefinition>
    }

    private static boolean containsIgnoreCase(String value, String lowerNeedle) {
        value != null && value.toLowerCase(Locale.ROOT).contains(lowerNeedle)
    }
}
