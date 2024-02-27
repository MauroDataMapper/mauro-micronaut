package uk.ac.ox.softeng.mauro.controller.terminology

class Paths {
    public static final String CODE_SETS = "/codeSets"
    public static final String CODE_SET_BY_ID = "/codeSets/{id}"
    public static final String ADD_TERM_TO_CODE_SET = "/codeSets/{id}/terms/{termId}"
    public static final String CODE_SETS_BY_FOLDER_ID = "/folders/{folderId}/codeSets"
    public static final String FINALISE_CODE_SETS = "/codeSets/{id}/finalise"
    public static final String CODE_SET_NEW_BRANCH_MODEL_VERSION = "/codeSets/{id}/newBranchModelVersion"
    public static final String CODE_SETS_BY_TERMINOLOGY_TERM = "/terminologies/{terminology_id}/terms/{term_id}/codeSets"
}
