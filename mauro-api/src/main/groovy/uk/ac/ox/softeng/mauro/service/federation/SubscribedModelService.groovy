package uk.ac.ox.softeng.mauro.service.federation

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.domain.facet.federation.MauroLink
import uk.ac.ox.softeng.mauro.domain.facet.federation.PublishedModel
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedModel
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedModelFederationParams
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.importdata.ImportMetadata
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository.SubscribedModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository
import uk.ac.ox.softeng.mauro.persistence.service.RepositoryService
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import uk.ac.ox.softeng.mauro.plugin.exporter.ModelExporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.FileImportParameters
import uk.ac.ox.softeng.mauro.plugin.importer.FileParameter
import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin
import uk.ac.ox.softeng.mauro.service.core.AuthorityService

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import jakarta.inject.Inject

@CompileStatic
@Slf4j
class SubscribedModelService {
    public static final String APPLICATION_CONTENT_TYPE = 'application/'
    public static final String MAURO_DOT = 'mauro.'
    public static final String PLUS = '\\+'
    final RepositoryService repositoryService
    final MauroPluginService mauroPluginService
    final SubscribedCatalogueService subscribedCatalogueService
    final ModelContentRepository<Model> modelContentRepository
    final ItemCacheableRepository.SubscribedModelCacheableRepository subscribedModelCacheableRepository
    final AuthorityService authorityService

    @Inject
    SubscribedModelService(RepositoryService repositoryService, MauroPluginService mauroPluginService, SubscribedCatalogueService subscribedCatalogueService,
                           ModelContentRepository<Model> modelContentRepository, SubscribedModelCacheableRepository subscribedModelCacheableRepository,
                           AuthorityService authorityService) {
        this.repositoryService = repositoryService
        this.mauroPluginService = mauroPluginService
        this.subscribedCatalogueService = subscribedCatalogueService
        this.modelContentRepository = modelContentRepository
        this.subscribedModelCacheableRepository = subscribedModelCacheableRepository
        this.authorityService = authorityService
    }


    PublishedModel getPublishedModelForSubscribedModel(SubscribedModel subscribedModel) {
        List<PublishedModel> publishedModels = subscribedCatalogueService.getPublishedModels(subscribedModel.subscribedCatalogue)

        // Atom feeds may allow multiple versions of an entry with the same ID
        return Optional.ofNullable(publishedModels
                                       .findAll {pm -> pm.modelId == subscribedModel.subscribedModelId}
                                       .sort {pm -> pm.lastUpdated}?.last()).orElse(null)
    }

    Model exportFederatedModelAndImport(SubscribedModelFederationParams subscribedModelFederationParams,
                                        Folder folder,
                                        SubscribedModel subscribedModel) {
        log.debug("Exporting SubscribedModel: $subscribedModel.subscribedModelId")

        PublishedModel sourcePublishedModel = getPublishedModelForSubscribedModel(subscribedModel)
        List<MauroLink> exportLinks = sourcePublishedModel?.links

        MauroLink exportLink = findExportLink(exportLinks, subscribedModelFederationParams)

        ModelImporterPlugin mauroImporterPlugin = getImporterPlugin(subscribedModelFederationParams, exportLink.contentType)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.BAD_REQUEST, mauroImporterPlugin, "$mauroImporterPlugin  not found ")

        ModelExporterPlugin exporterPlugin = getExporterPlugin(subscribedModel.subscribedModelType)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.BAD_REQUEST, exporterPlugin, "$exporterPlugin  not found ")


        byte[] resourceBytes = subscribedCatalogueService.getBytesResourceExport(subscribedModel.subscribedCatalogue, exportLink.url)
        FileParameter fileParameter = new FileParameter(null, exportLink.contentType, resourceBytes)

        FileImportParameters fileImportParameters = new FileImportParameters().tap {
            importFile = fileParameter
            folderId = folder.id
            finalised = true
            useDefaultAuthority = false
            importAsNewBranchModelVersion = sourcePublishedModel?.modelVersion ? false : true
        }

        //grails compares remote and defaultAuthority -not in micronaut FileImportParams
        List<Model> importedModels = (List<Model>) mauroImporterPlugin.importModels(fileImportParameters)
        Model savedImported = importedModels?.first()
        checkModelLabelAndVersionNotAlreadyImported(savedImported)
        if (savedImported) {
            savedImported.folder = folder
            modelContentRepository.saveWithContent(savedImported as Model)
        }
    }


    private ModelExporterPlugin getExporterPlugin(String subscribedModelType) {
        mauroPluginService.listPlugins(ModelExporterPlugin).find {
            ModelExporterPlugin.isInstance(it) && it.providerType.contains(subscribedModelType)
        }
    }

    private MauroLink findExportLink(List<MauroLink> exportLinks, SubscribedModelFederationParams subscribedModelFederationParams) {
        return Optional.ofNullable(exportLinks.find {link ->
            (!subscribedModelFederationParams.contentType || (subscribedModelFederationParams.contentType == link.contentType))
        }).orElseThrow(() ->
                           ErrorHandler
                               .handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Import failed, no published link found for URL $subscribedModelFederationParams.url"))
    }

    private ModelImporterPlugin getImporterPlugin(SubscribedModelFederationParams subscribedModelFederationParams, String contentType) {
        if (subscribedModelFederationParams.importMetadata) {
            getImporterPlugin(subscribedModelFederationParams.importMetadata)
        } else {
            getImporterPluginForContentTypeOrUrl(contentType)
        }
    }

    private ModelImporterPlugin getImporterPlugin(ImportMetadata importMetadata) {
        if (importMetadata.allFieldsPresent()) {
            mauroPluginService.getPlugin(ModelImporterPlugin, importMetadata.namespace, importMetadata.name, importMetadata.version)
        } else if (importMetadata.hasName() && importMetadata.hasNameSpace()) {
            mauroPluginService.getPlugin(ModelImporterPlugin, importMetadata.namespace, importMetadata.name)
        } else null
    }

    private ModelImporterPlugin getImporterPluginForContentTypeOrUrl(String contentType) {
        String modelType = contentType.split(APPLICATION_CONTENT_TYPE)[1].split(MAURO_DOT)[1].split(PLUS)[0].toLowerCase()
        if (!modelType) {
            modelType
        }
        mauroPluginService.listPlugins(ModelImporterPlugin).find {
            ModelImporterPlugin.isInstance(it) && it.providerType.toLowerCase().contains(modelType)
        }
    }

    void checkModelLabelAndVersionNotAlreadyImported(Model model) {
        Model existing = repositoryService.modelCacheableRepositories.find {it.domainType == model.domainType}
            .readByLabelAndModelVersion(model.label, model.modelVersion)
        if (existing != null) {
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Model already exists with label $model.label and model version $model.modelVersion")
        }
    }

    Long deleteModels(SubscribedCatalogue subscribedCatalogue) {
        subscribedModelCacheableRepository.deleteAll(subscribedModelCacheableRepository.findAllBySubscribedCatalogueId(subscribedCatalogue.id))

    }
}

