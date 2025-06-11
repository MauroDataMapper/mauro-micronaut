package org.maurodata.controller.folder

import com.fasterxml.jackson.core.Versioned
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import org.maurodata.ErrorHandler
import org.maurodata.api.Paths
import org.maurodata.api.folder.VersionedFolderApi
import org.maurodata.api.model.MergeDiffDTO
import org.maurodata.api.model.MergeFieldDiffDTO
import org.maurodata.api.model.ModelVersionDTO
import org.maurodata.api.model.ModelVersionedRefDTO
import org.maurodata.api.model.ModelVersionedWithTargetsRefDTO
import org.maurodata.api.model.PermissionsDTO
import org.maurodata.api.model.VersionLinkTargetDTO
import org.maurodata.audit.Audit
import org.maurodata.controller.model.ModelController
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.diff.FieldDiff
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.facet.VersionLink
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.folder.FolderService
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.Path
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.model.version.ModelVersion
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.persistence.folder.FolderContentRepository
import org.maurodata.web.ListResponse

@Slf4j
@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class VersionedFolderController extends ModelController<Folder> implements VersionedFolderApi {

    private static final String MY_CLASS_TYPE = "VersionedFolder"

    @Inject
    FolderContentRepository folderContentRepository

    @Inject
    FolderService folderService

    VersionedFolderController(FolderCacheableRepository folderRepository, FolderContentRepository folderContentRepository, FolderService folderService) {
        super(Folder, folderRepository, folderRepository, folderContentRepository, folderService)
        this.folderService = folderService
    }

    @Get(Paths.VERSIONED_FOLDER_ID)
    Folder show(UUID id) {
        super.show(id)
    }

    @Get(Paths.FOLDER_CHILD_VERSIONED_FOLDER_ID)
    Folder show(UUID parentId, UUID id) {
        super.show(id)
    }

    @Audit
    @Post(Paths.VERSIONED_FOLDER_LIST)
    Folder create(@Body Folder folder) {
        cleanBody(folder)
        updateCreationProperties(folder)

        folder.setVersionable(true)

        pathRepository.readParentItems(folder)
        folder.updatePath()

        folderRepository.save(folder)
    }

    @Audit
    @Transactional
    @Post(Paths.CHILD_VERSIONED_FOLDER_LIST)
    Folder create(UUID parentId, @Body @NonNull Folder folder) {
        folder.setVersionable(true)
        super.create(parentId, folder)
    }

    @Audit
    @Put(Paths.VERSIONED_FOLDER_ID)
    Folder update(UUID id, @Body @NonNull Folder folder) {
        super.update(id, folder)
    }

    @Audit
    @Put(Paths.FOLDER_CHILD_VERSIONED_FOLDER_ID)
    Folder update(UUID parentId, UUID id, @Body @NonNull Folder folder) {
        super.update(id, folder)
    }

    @Get(Paths.VERSIONED_FOLDER_LIST)
    ListResponse<Folder> listAll() {

        final ListResponse<Folder> listResponse = super.listAll()

        listResponse.items = listResponse.items.findAll {MY_CLASS_TYPE == ((Folder) it).getClass_()}
        listResponse.count = listResponse.items.size()

        return listResponse
    }

    @Get(Paths.CHILD_VERSIONED_FOLDER_LIST)
    ListResponse<Folder> list(UUID parentId) {

        final ListResponse<Folder> listResponse = super.list(parentId)

        listResponse.items = listResponse.items.findAll {MY_CLASS_TYPE == ((Folder) it).getClass_()}
        listResponse.count = listResponse.items.size()

        return listResponse
    }

    @Audit
    @Transactional
    @Put(Paths.VERSIONED_FOLDER_FINALISE)
    Folder finalise(UUID id, @Body FinaliseData finaliseData) {
        super.finalise(id, finaliseData)
    }

    @Audit
    @Transactional
    @Put(Paths.VERSIONED_FOLDER_NEW_BRANCH_MODEL_VERSION)
    Folder createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        super.createNewBranchModelVersion(id, createNewVersionData)
    }

    @Audit
    @Transactional
    @Delete(Paths.VERSIONED_FOLDER_ID)
    HttpResponse delete(UUID id, @Body @Nullable Folder folder, @Nullable @QueryValue Boolean permanent) {
        permanent = permanent ?: true
        super.delete(id, folder, permanent)
    }

    @Audit
    @Transactional
    @Delete(Paths.FOLDER_CHILD_VERSIONED_FOLDER_ID)
    HttpResponse delete(UUID parentId, UUID id, @Body @Nullable Folder folder, @Nullable @QueryValue Boolean permanent) {
        permanent = permanent ?: true
        super.delete(id, folder, permanent)
    }

    @Audit
    @Put(Paths.VERSIONED_FOLDER_READ_BY_AUTHENTICATED)
    @Transactional
    Folder allowReadByAuthenticated(UUID id) {
        super.putReadByAuthenticated(id) as Folder
    }

    @Audit
    @Transactional
    @Delete(Paths.VERSIONED_FOLDER_READ_BY_AUTHENTICATED)
    HttpResponse revokeReadByAuthenticated(UUID id) {
        super.deleteReadByAuthenticated(id)
    }

    @Audit
    @Put(Paths.VERSIONED_FOLDER_READ_BY_EVERYONE)
    @Transactional
    Folder allowReadByEveryone(UUID id) {
        super.putReadByEveryone(id) as Folder
    }

    @Audit
    @Transactional
    @Delete(Paths.VERSIONED_FOLDER_READ_BY_EVERYONE)
    HttpResponse revokeReadByEveryone(UUID id) {
        super.deleteReadByEveryone(id)
    }

    @Get(Paths.VERSIONED_FOLDER_PERMISSIONS)
    @Override
    PermissionsDTO permissions(UUID id) {
        super.permissions(id)
    }

    @Get(Paths.VERSIONED_FOLDER_DOI)
    @Override
    Map doi(UUID id) {
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Doi is not implemented")
        return null
    }

    @Override
    @Get(Paths.VERSIONED_FOLDER_SIMPLE_MODEL_VERSION_TREE)
    List<ModelVersionedRefDTO> simpleModelVersionTree(UUID id, @Nullable Boolean branchesOnly) {

        branchesOnly = branchesOnly ?: false

        final List<Model> allModels = populateVersionTree(id, branchesOnly, null)

        // Create object DTOs

        final ArrayList<ModelVersionedRefDTO> simpleModelVersionTreeList = new ArrayList<>(allModels.size())

        allModels.each {Model model ->
            simpleModelVersionTreeList.add(
                new ModelVersionedRefDTO(id: model.id, branch: model.branchName, branchName: model.branchName, modelVersion: model.modelVersion?.toString(),
                                         modelVersionTag: model.modelVersionTag, documentationVersion: model.documentationVersion, displayName: model.pathModelIdentifier))
        }

        simpleModelVersionTreeList
    }

    @Override
    @Get(Paths.VERSIONED_FOLDER_MODEL_VERSION_TREE)
    List<ModelVersionedWithTargetsRefDTO> modelVersionTree(UUID id) {

        final Map<UUID, Map<String, Boolean>> flags = [:]
        final List<Model> allModels = super.populateVersionTree(id, false, flags)

        // Create object DTOs

        final ArrayList<ModelVersionedWithTargetsRefDTO> modelVersionTreeList = new ArrayList<>(allModels.size())

        for (Model model : allModels) {
            final ModelVersionedWithTargetsRefDTO modelVersionedWithTargetsRefDTO = new ModelVersionedWithTargetsRefDTO(id: model.id, branch: model.branchName,
                                                                                                                        branchName: model.branchName,
                                                                                                                        modelVersion: model.modelVersion?.toString(),
                                                                                                                        modelVersionTag: model.modelVersionTag,
                                                                                                                        documentationVersion: model.documentationVersion,
                                                                                                                        displayName: model.pathModelIdentifier)

            // Have any flags been set during recursion?
            final Map<String, Boolean> modelFlags = flags.get(modelVersionedWithTargetsRefDTO.id)

            if (modelFlags != null) {
                Boolean isNewBranchModelVersion = modelFlags.get("isNewBranchModelVersion")
                Boolean isNewFork = modelFlags.get("isNewFork")

                if (isNewBranchModelVersion != null && isNewBranchModelVersion) {
                    modelVersionedWithTargetsRefDTO.isNewBranchModelVersion = true
                } else if (isNewFork != null && isNewFork) {
                    modelVersionedWithTargetsRefDTO.isNewFork = true
                }
            }

            // Add in targets

            if (model.versionLinks != null && !model.versionLinks.isEmpty()) {
                for (VersionLink childVersion : model.versionLinks) {
                    final UUID targetModelId = childVersion.targetModelId
                    if (targetModelId == null) {
                        continue
                    }

                    final VersionLinkTargetDTO versionLinkTargetDTO = new VersionLinkTargetDTO(id: targetModelId, description: childVersion.description)

                    modelVersionedWithTargetsRefDTO.targets.add(versionLinkTargetDTO)
                }
            }

            modelVersionTreeList.add(modelVersionedWithTargetsRefDTO)
        }

        modelVersionTreeList
    }

    @Override
    @Get(Paths.VERSIONED_FOLDER_CURRENT_MAIN_BRANCH)
    Folder currentMainBranch(UUID id) {

        // Like Highlander, there can be only one
        // main/draft version
        // and it downstream

        final List<Model> allModels = populateVersionTree(id, true, null)

        for (int m = allModels.size() - 1; m >= 0; m--) {
            final Model givenModel = allModels.get(m)
            if (givenModel.branchName == Model.DEFAULT_BRANCH_NAME) {
                return givenModel as Folder
            }
        }

        throw new HttpStatusException(HttpStatus.NOT_FOUND, "There is no main draft model")
    }

    @Override
    @Get(Paths.VERSIONED_FOLDER_LATEST_MODEL_VERSION)
    ModelVersionDTO latestModelVersion(UUID id) {
        final List<Model> allModels = populateVersionTree(id, false, null)

        if (allModels.size() == 0) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "There are no models")
        }

        ModelVersion highestVersion = null

        for (int m = 0; m < allModels.size(); m++) {
            final Model givenModel = allModels.get(m)
            final ModelVersion modelVersion = givenModel.modelVersion
            if (modelVersion == null) {
                continue
            }

            if (highestVersion == null) {
                highestVersion = modelVersion
                continue
            }

            if (modelVersion > highestVersion) {
                highestVersion = modelVersion
            }
        }

        if (highestVersion == null) {
            new ModelVersionDTO(modelVersion: "0.0.0")
        }

        return new ModelVersionDTO(modelVersion: highestVersion)
    }

    @Override
    @Get(Paths.VERSIONED_FOLDER_LATEST_FINALISED_MODEL)
    ModelVersionedRefDTO latestFinalisedModel(UUID id) {
        final List<Model> allModels = populateVersionTree(id, false, null)

        if (allModels.size() == 0) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "There are no models")
        }

        Model highestVersionModel = null

        for (int m = 0; m < allModels.size(); m++) {
            final Model givenModel = allModels.get(m)
            final ModelVersion modelVersion = givenModel.modelVersion
            if (modelVersion == null) {
                continue
            }

            if (highestVersionModel == null) {
                highestVersionModel = givenModel
                continue
            }

            if (modelVersion > highestVersionModel.modelVersion) {
                highestVersionModel = givenModel
            }
        }

        if (highestVersionModel == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "There are no versioned models")
        }

        return new ModelVersionedRefDTO(id: highestVersionModel.id, domainType: highestVersionModel.domainType, label: highestVersionModel.label,
                                        branch: highestVersionModel.branchName, branchName: highestVersionModel.branchName,
                                        modelVersion: highestVersionModel.modelVersion?.toString(), modelVersionTag: highestVersionModel.modelVersionTag,
                                        documentationVersion: highestVersionModel.documentationVersion, displayName: highestVersionModel.pathModelIdentifier)
    }

    @Get(Paths.VERSIONED_FOLDER_COMMON_ANCESTOR)
    Folder commonAncestor(UUID id, UUID other_model_id) {
        final Folder left = show(id)
        if (!left) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "No data model found for id [${left.id.toString()}]")
        }
        final Folder right = show(other_model_id)
        if (!right) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "No data model found for id [${right.id.toString()}]")
        }

        return findCommonAncestorBetweenModels(left, right)
    }

    @Get(Paths.VERSIONED_FOLDER_MERGE_DIFF)
    MergeDiffDTO mergeDiff(@NonNull UUID id, @NonNull UUID otherId) {
        final Folder dataModelOne = modelContentRepository.findWithContentById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataModelOne, "item with $id not found")

        final Folder dataModelTwo = modelContentRepository.findWithContentById(otherId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataModelTwo, "item with $otherId not found")

        accessControlService.checkRole(Role.READER, dataModelOne)
        accessControlService.checkRole(Role.READER, dataModelTwo)

        final Folder commonAncestor = findCommonAncestorBetweenModels(dataModelOne, dataModelTwo)

        dataModelOne.setAssociations()
        dataModelTwo.setAssociations()
        commonAncestor.setAssociations()

        // For a merge, there are two diffs between the common ancestor and model being merged

        final ObjectDiff objectDiffOne = commonAncestor.diff(dataModelOne)
        final ObjectDiff objectDiffTwo = commonAncestor.diff(dataModelTwo)

        // Remove branchName differences since these are merges of branches

        for (ObjectDiff objectDiff : ([objectDiffOne, objectDiffTwo] as List<ObjectDiff>)) {
            final ArrayList<FieldDiff> diffsToRemove = new ArrayList<>(2)
            for (fieldDiff in objectDiff.diffs) {
                if (fieldDiff.name == 'branchName') {
                    diffsToRemove.add(fieldDiff)
                }
            }
            objectDiff.diffs.removeAll(diffsToRemove)
        }

        // recurse down through each diff and create a map of Path-> FieldDiff

        Map<String, Map<String, FieldDiff>> flattenedDiffOne = flattenDiff(objectDiffOne)
        Map<String, Map<String, FieldDiff>> flattenedDiffTwo = flattenDiff(objectDiffTwo)

        // Collate the set of all paths used as keys

        final Map<String, FieldDiff> flattenedDiffOneCreated = flattenedDiffOne.get('created')
        final Map<String, FieldDiff> flattenedDiffOneModified = flattenedDiffOne.get('modified')
        final Map<String, FieldDiff> flattenedDiffOneDeleted = flattenedDiffOne.get('deleted')

        final Map<String, FieldDiff> flattenedDiffTwoCreated = flattenedDiffTwo.get('created')
        final Map<String, FieldDiff> flattenedDiffTwoModified = flattenedDiffTwo.get('modified')
        final Map<String, FieldDiff> flattenedDiffTwoDeleted = flattenedDiffTwo.get('deleted')

        final List<Map<String, FieldDiff>> flattenedDiffOneMapList = [flattenedDiffOneCreated, flattenedDiffOneModified, flattenedDiffOneDeleted]
        final List<Map<String, FieldDiff>> flattenedDiffTwoMapList = [flattenedDiffTwoCreated, flattenedDiffTwoModified, flattenedDiffTwoDeleted]

        final Set<String> allPaths = new LinkedHashSet<>()
        for (Map<String, FieldDiff> pathToFieldDiff : flattenedDiffOneMapList + flattenedDiffTwoMapList) {
            final Set keys = pathToFieldDiff.keySet()
            allPaths.addAll(keys)
        }

        List<Path.PathNode> pathNodes = []

        AdministeredItem node = dataModelOne
        pathNodes.add(0, new Path.PathNode(prefix: node.pathPrefix, identifier: node.pathIdentifier, modelIdentifier: node.pathModelIdentifier))

        final Path path = new Path(pathNodes)
        final String sourcePath = path.toString()

        final List<MergeFieldDiffDTO> diffs = []
        final MergeDiffDTO mergeDiffDTO = new MergeDiffDTO(sourceId: dataModelOne.id, targetId: dataModelTwo.id, path: sourcePath, label: dataModelOne.label, diffs: diffs)

        // for each key, compare what needs to be done to merge
        // changes are only in conflict when the value of the ancestor, branch one, branch two all have
        // different values e.g. there are 3 distinct values
        // For each distinct path, look up the value held in each of the three places and add it to a set
        // if the set has 3 values, there is a conflict, otherwise it a simple change

        for (String key : allPaths) {
            FieldDiff fieldDiffOne = null
            String _type = 'modification'
            lookingInOne:
            for (Map<String, FieldDiff> pathToFieldDiff : flattenedDiffOneMapList) {
                final foundFieldDiff = pathToFieldDiff.get(key)
                if (foundFieldDiff != null) {
                    fieldDiffOne = foundFieldDiff
                    if (pathToFieldDiff == flattenedDiffOneCreated) {
                        _type = 'creation'
                    } else if (pathToFieldDiff == flattenedDiffOneDeleted) {
                        _type = 'deletion'
                    }
                    break lookingInOne
                }
            }

            FieldDiff fieldDiffTwo = null
            lookingInTwo:
            for (Map<String, FieldDiff> pathToFieldDiff : flattenedDiffTwoMapList) {
                final foundFieldDiff = pathToFieldDiff.get(key)
                if (foundFieldDiff != null) {
                    fieldDiffTwo = foundFieldDiff
                    break lookingInTwo
                }
            }

            // Find the common ancestor version
            // In the FieldDiff lhs is the value object from the commonAncestor, rhs is a branch

            final Object fieldOneValue, fieldTwoValue, ancestorValue
            final String fieldName

            // Establish the ancestorValue first, since it is the backstop value

            if (fieldDiffOne != null) {
                ancestorValue = fieldDiffOne.left
                fieldName = fieldDiffOne.name
            } else if (fieldDiffTwo != null) {
                ancestorValue = fieldDiffTwo.left
                fieldName = fieldDiffTwo.name
            } else {
                ancestorValue = null
                fieldName = null
            }

            // If there is a change, record the value
            // or else it is the same as the ancestor value

            if (fieldDiffOne != null) {
                fieldOneValue = fieldDiffOne.right
            } else {
                fieldOneValue = ancestorValue
            }

            if (fieldDiffTwo != null) {
                fieldTwoValue = fieldDiffTwo.right
            } else {
                fieldTwoValue = ancestorValue
            }

            // If a fieldValueOne or fieldValueTwo value do not exist and the ancestor value does, they are
            // no different from the ancestor value
            final HashSet<Object> values = [fieldOneValue, fieldTwoValue, ancestorValue]

            final boolean isMergeConflict = values.size() == 3

            // Because the target branch is branch two, we are only interested in
            // changes or conflicts coming from the source branch (branch one)
            // anything that is not a conflict from branch two is a no-op
            //                 | ---------------(Branch Two)-->
            //  (ancestor) --> | ---(Branch One)----^

            if (!isMergeConflict && fieldDiffOne == null) {
                continue
            }

            final MergeFieldDiffDTO mergeFieldDiffDTO = new MergeFieldDiffDTO(fieldName: fieldName, sourceValue: fieldOneValue, targetValue: fieldTwoValue,
                                                                              commonAncestorValue: ancestorValue, isMergeConflict: isMergeConflict, _type: _type)

            diffs.add(mergeFieldDiffDTO)

        }

        return mergeDiffDTO
    }

}