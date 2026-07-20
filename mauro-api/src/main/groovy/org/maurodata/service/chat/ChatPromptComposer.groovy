package org.maurodata.service.chat

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class ChatPromptComposer {

    private final ChatPromptAssetService promptAssetService
    private final ChatPromptAssetService fallbackAssetService

    ChatPromptComposer(ChatPromptAssetService promptAssetService) {
        this.fallbackAssetService = new ChatPromptAssetRegistryService(new ChatPromptAssetDefinitionLoader())
        this.promptAssetService = promptAssetService ?: fallbackAssetService
    }

    String render(String assetId, Map<String, Object> variables = Collections.<String, Object>emptyMap(), String fallback = '') {
        renderResult(assetId, variables, fallback).text
    }

    ChatPromptRenderResult renderResult(String assetId, Map<String, Object> variables = Collections.<String, Object>emptyMap(), String fallback = '') {
        ChatPromptAssetDefinition asset = promptAssetService?.findAsset(assetId)
        boolean fallbackWasUsed = false
        if ((asset == null || asset.instruction == null || asset.instruction.trim().isEmpty()) && promptAssetService != fallbackAssetService) {
            asset = fallbackAssetService.findAsset(assetId)
            fallbackWasUsed = true
        }
        List<Map<String, Object>> fragmentMetadata = new ArrayList<Map<String, Object>>()
        String template = renderTemplateWithFragments(asset, fallback, fragmentMetadata)
        String rendered = applyVariables(template, variables)
        new ChatPromptRenderResult(
            text: rendered.trim(),
            assetId: asset?.id ?: assetId,
            assetVersion: asset?.version,
            assetType: asset?.type,
            fallbackUsed: fallbackWasUsed || asset == null,
            fragments: fragmentMetadata,
            variableNames: new ArrayList<String>((variables ?: Collections.<String, Object>emptyMap()).keySet()).sort(false),
            redactedVariableNames: redactedVariableNames(variables)
        )
    }

    private String renderTemplateWithFragments(ChatPromptAssetDefinition asset, String fallback, List<Map<String, Object>> fragmentMetadata) {
        String template = null
        if (asset != null && asset.instruction != null && !asset.instruction.trim().isEmpty()) {
            template = asset.instruction.trim()
        }
        if (template == null || template.trim().isEmpty()) {
            template = fallback ?: ''
        }

        List<String> renderedFragments = []
        for (String fragmentId : asset?.fragments ?: [] as List<String>) {
            String fragment = renderFragment(fragmentId, new LinkedHashSet<String>(), fragmentMetadata)
            if (fragment != null && !fragment.trim().isEmpty()) {
                renderedFragments.add(fragment.trim())
            }
        }
        renderedFragments.isEmpty() ? template : (renderedFragments.join('\n\n') + '\n\n' + template)
    }

    private String renderFragment(String fragmentId, Set<String> seen, List<Map<String, Object>> fragmentMetadata) {
        if (fragmentId == null || fragmentId.trim().isEmpty()) {
            return ''
        }
        if (!seen.add(fragmentId)) {
            throw new IllegalStateException("Prompt fragment cycle detected: ${seen.join(' -> ')} -> ${fragmentId}")
        }
        ChatPromptAssetDefinition fragment = promptAssetService?.findAsset(fragmentId)
        if ((fragment == null || fragment.instruction == null || fragment.instruction.trim().isEmpty()) && promptAssetService != fallbackAssetService) {
            fragment = fallbackAssetService.findAsset(fragmentId)
        }
        if (fragment == null) {
            throw new IllegalArgumentException("Unknown prompt fragment: ${fragmentId}")
        }
        if (!'FRAGMENT'.equalsIgnoreCase(fragment.type)) {
            throw new IllegalArgumentException("Prompt asset ${fragmentId} is ${fragment.type}, expected FRAGMENT")
        }
        fragmentMetadata.add([
            id     : fragment.id,
            version: fragment.version,
            type   : fragment.type
        ] as Map<String, Object>)
        List<String> nested = []
        for (String nestedId : fragment.fragments ?: [] as List<String>) {
            String rendered = renderFragment(nestedId, seen, fragmentMetadata)
            if (rendered != null && !rendered.trim().isEmpty()) {
                nested.add(rendered.trim())
            }
        }
        if (fragment.instruction != null && !fragment.instruction.trim().isEmpty()) {
            nested.add(fragment.instruction.trim())
        }
        seen.remove(fragmentId)
        nested.join('\n\n')
    }

    private static String applyVariables(String template, Map<String, Object> variables) {
        String rendered = template
        for (Map.Entry<String, Object> entry : (variables ?: Collections.<String, Object>emptyMap()).entrySet()) {
            rendered = rendered.replace('{{' + entry.key + '}}', entry.value == null ? '' : String.valueOf(entry.value))
        }
        rendered
    }

    private static List<String> redactedVariableNames(Map<String, Object> variables) {
        (variables ?: Collections.<String, Object>emptyMap()).keySet().findAll {String key ->
            String lower = key == null ? '' : key.toLowerCase(Locale.ROOT)
            lower.contains('token') || lower.contains('secret') || lower.contains('password') || lower.contains('credential')
        }.sort(false) as List<String>
    }
}
