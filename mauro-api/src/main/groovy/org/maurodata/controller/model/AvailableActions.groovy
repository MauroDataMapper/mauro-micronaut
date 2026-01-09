package org.maurodata.controller.model

import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.ModelItem
import org.maurodata.domain.security.Role
import org.maurodata.domain.security.SecurableResource
import org.maurodata.security.AccessControlService

import groovy.transform.CompileStatic

@CompileStatic
class AvailableActions {

    // Purposes
    public static String PURPOSE_VERSIONING = "VERSIONING"
    public static String PURPOSE_STANDARD = "STANDARD"
    public static String PURPOSE_SECURABLE = "SECURABLE"
    public static String PURPOSE_MODEL = "MODEL"
    public static String PURPOSE_FOLDER = "FOLDER"
    public static String PURPOSE_DATAMODEL = "DATAMODEL"
    public static String PURPOSE_MODELITEM = "MODELITEM"

    // Actions
    public static final String SHOW_ACTION = 'show'
    public static final String UPDATE_ACTION = 'update'
    public static final String DELETE_ACTION = 'delete'
    public static final String DISABLE_ACTION = 'disable'
    public static final String SAVE_ACTION = 'save'
    public static final String COMMENT_ACTION = 'comment'
    public static final String EDIT_DESCRIPTION_ACTION = 'editDescription'
    public static final String SOFT_DELETE_ACTION = 'softDelete'
    public static final String NEW_DOCUMENTATION_ACTION = 'newDocumentationVersion'
    public static final String NEW_BRANCH_MODEL_VERSION_ACTION = 'newBranchModelVersion'
    public static final String FINALISE_ACTION = 'finalise'
    public static final String CREATE_NEW_VERSIONS_ACTION = 'createNewVersions'
    public static final String NEW_FORK_MODEL_ACTION = 'newForkModel'
    public static final String NEW_MODEL_VERSION_ACTION = 'newModelVersion'
    public static final String MERGE_INTO_ACTION = 'mergeInto'

    public static final String READ_BY_EVERYONE_ACTION = 'readByEveryone'
    public static final String READ_BY_AUTHENTICATED_ACTION = 'readByAuthenticated'

    public static final String CHANGE_FOLDER_ACTION = 'changeFolder'
    public static final String CREATE_FOLDER = 'createFolder'
    public static final String CREATE_VERSIONED_FOLDER = 'createVersionedFolder'
    public static final String CREATE_CONTAINER = 'createContainer'
    public static final String CREATE_MODEL = 'createModel'
    public static final String CREATE_MODEL_ITEM = 'createModelItem'
    public static final String MOVE_TO_FOLDER = 'moveToFolder'
    public static final String MOVE_TO_VERSIONED_FOLDER = 'moveToVersionedFolder'
    public static final String MOVE_TO_CONTAINER = 'moveToContainer'

    public static final String SUBSET_ACTION = 'subset'
    public static final String FINALISED_EDIT_ACTIONS = 'finalisedEditActions'
    public static final String FINALISED_READ_ACTIONS = 'finalisedReadActions'
    public static final String SAVE_IGNORE_FINALISE = 'saveIgnoreFinalise'
    public static final String UPDATE_IGNORE_FINALISE = 'updateIgnoreFinalise'


    static Map<String, Map<Role, List<String>>> PurposeRoleActions = [:]

    static {

        {
            Map<Role, List<String>> roleActions = [:]

            roleActions.put(Role.READER, [CREATE_NEW_VERSIONS_ACTION, NEW_FORK_MODEL_ACTION])
            roleActions.put(Role.EDITOR, [NEW_MODEL_VERSION_ACTION,
                                          NEW_DOCUMENTATION_ACTION,
                                          NEW_BRANCH_MODEL_VERSION_ACTION,
                                          FINALISE_ACTION,
                                          MERGE_INTO_ACTION
            ])
            roleActions.put(Role.CONTAINER_ADMIN, [
                    FINALISED_EDIT_ACTIONS,
                    FINALISED_READ_ACTIONS,
                    SAVE_IGNORE_FINALISE,
                    UPDATE_IGNORE_FINALISE])
            PurposeRoleActions.put(PURPOSE_VERSIONING, roleActions)
        }

        {
            Map<Role, List<String>> roleActions = [:]

            roleActions.put(Role.READER, [SHOW_ACTION])
            roleActions.put(Role.REVIEWER, [COMMENT_ACTION])
            roleActions.put(Role.AUTHOR, [EDIT_DESCRIPTION_ACTION])
            roleActions.put(Role.EDITOR, [UPDATE_ACTION, SOFT_DELETE_ACTION, SAVE_ACTION])
            roleActions.put(Role.CONTAINER_ADMIN, [DELETE_ACTION, DISABLE_ACTION])

            PurposeRoleActions.put(PURPOSE_STANDARD, roleActions)
        }

        {
            Map<Role, List<String>> roleActions = [:]

            roleActions.put(Role.CONTAINER_ADMIN, [READ_BY_EVERYONE_ACTION,
                                                   READ_BY_AUTHENTICATED_ACTION,
                                                   SHOW_ACTION,
                                                   UPDATE_ACTION,
                                                   DELETE_ACTION,
                                                   SAVE_ACTION])

            PurposeRoleActions.put(PURPOSE_SECURABLE, roleActions)
        }

        {
            Map<Role, List<String>> roleActions = [:]

            roleActions.put(Role.READER, [SUBSET_ACTION])

            PurposeRoleActions.put(PURPOSE_MODEL, roleActions)
        }

        {
            Map<Role, List<String>> roleActions = [:]

            roleActions.put(Role.EDITOR, [CHANGE_FOLDER_ACTION, CREATE_FOLDER, CREATE_VERSIONED_FOLDER, CREATE_MODEL, MOVE_TO_FOLDER, MOVE_TO_VERSIONED_FOLDER])

            PurposeRoleActions.put(PURPOSE_FOLDER, roleActions)
        }

        {
            Map<Role, List<String>> roleActions = [:]

            roleActions.put(Role.EDITOR, [MOVE_TO_FOLDER, MOVE_TO_CONTAINER, MOVE_TO_VERSIONED_FOLDER, CREATE_MODEL_ITEM])

            PurposeRoleActions.put(PURPOSE_DATAMODEL, roleActions)
        }

        {
            Map<Role, List<String>> roleActions = [:]

            roleActions.put(Role.EDITOR, [CREATE_MODEL_ITEM, DELETE_ACTION])

            PurposeRoleActions.put(PURPOSE_MODELITEM, roleActions)
        }

    }

    public static List<String> REMOVE_WHEN_FINALISED = [UPDATE_ACTION,
                                                        SAVE_ACTION,
                                                        EDIT_DESCRIPTION_ACTION,
                                                        FINALISE_ACTION,
                                                        MERGE_INTO_ACTION,
                                                        CREATE_FOLDER,
                                                        CREATE_VERSIONED_FOLDER,
                                                        CREATE_MODEL]

    public static List<String> REMOVE_FROM_MODEL_ITEM = [SOFT_DELETE_ACTION,
                                                         FINALISE_ACTION]

    private static final List<String> empty_list = []


    static List<String> getActionsForRolePurpose(final Role role, final String purpose) {
        final Map<Role, List<String>> roleActions = PurposeRoleActions.get(purpose)
        if (roleActions == null) {
            return empty_list
        }

        final List<String> actions = roleActions.get(role)
        if (actions != null) {
            return actions
        }

        return empty_list
    }

    static List<String> getActionsForRolesPurpose(final List<Role> roles, final String purpose) {
        final List<String> allActions = []

        for (role in roles) {
            allActions.addAll(getActionsForRolePurpose(role, purpose))
        }

        return allActions
    }

    static void updateAvailableActions(AdministeredItem item, AccessControlService accessControlService) {

        if (item.availableActions == null) {
            item.availableActions = []
        }

        final List<Role> roles = accessControlService.listCanDoRoles(item)

        item.availableActions.clear()

        // Additions

        item.availableActions.addAll(getActionsForRolesPurpose(roles, PURPOSE_STANDARD))

        if (item instanceof SecurableResource) {
            item.availableActions.addAll(getActionsForRolesPurpose(roles, PURPOSE_SECURABLE))
        }

        if (item instanceof Folder) {
            item.availableActions.addAll(getActionsForRolesPurpose(roles, PURPOSE_FOLDER))
        }

        if (item instanceof DataModel) {
            item.availableActions.addAll(getActionsForRolesPurpose(roles, PURPOSE_DATAMODEL))
        }

        if (item instanceof ModelItem) {
            item.availableActions.addAll(getActionsForRolesPurpose(roles, PURPOSE_MODELITEM))
        }

        if (item instanceof Model) {
            final Model itemAsModel = (Model) item
            if (itemAsModel.branchName || itemAsModel.modelVersion) {
                item.availableActions.addAll(getActionsForRolesPurpose(roles, PURPOSE_VERSIONING))
            }
            item.availableActions.addAll(getActionsForRolesPurpose(roles, PURPOSE_MODEL))
        }

        // Removals

        if (item instanceof Model) {
            final Model itemAsModel = (Model) item

            if (itemAsModel.finalised) {
                item.availableActions.removeAll(REMOVE_WHEN_FINALISED)
            }
        }

        if (item instanceof ModelItem) {
            item.availableActions.removeAll(REMOVE_FROM_MODEL_ITEM)
        }

        // TODO: mergeInto is removed if not a non-main draft

        // It also doesn't appear to be offered for VersionedFolders
        // in the grail implementation
    }


}
