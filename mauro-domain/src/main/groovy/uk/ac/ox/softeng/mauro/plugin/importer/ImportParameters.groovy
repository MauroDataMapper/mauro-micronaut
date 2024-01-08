package uk.ac.ox.softeng.mauro.plugin.importer

import uk.ac.ox.softeng.mauro.plugin.importer.config.ImportGroupConfig
import uk.ac.ox.softeng.mauro.plugin.importer.config.ImportParameterConfig

abstract class ImportParameters {

    @ImportParameterConfig(
        displayName = 'Folder',
        description = 'The folder into which the Model/s should be imported.',
        order = 4,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    UUID folderId

    @ImportParameterConfig(
        optional = true,
        displayName = 'Model name',
        description = ['Label of Model, this will override any existing name provided in the imported data.',
            'Note that if importing multiple models this will be ignored.'],
        descriptionJoinDelimiter = ' ',
        order = 3,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    String modelName

    @ImportParameterConfig(
        displayName = 'Finalised',
        description = ['Whether the new model is to be marked as finalised.',
            'Note that if the model is already finalised this will not be overridden.'],
        descriptionJoinDelimiter = ' ',
        order = 2,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    Boolean finalised = false

    @ImportParameterConfig(
        displayName = 'Import as New Documentation Version',
        description = [
            'Should the Model/s be imported as new Documentation Version/s.',
            'If selected then any models with the same name will be superseded and the imported models will be given the latest',
            'documentation version of the existing Models.',
            'If not selected then the \'Model Name\' field should be used to ensure the imported Model is uniquely named,',
            'otherwise you could get an error.'],
        descriptionJoinDelimiter = ' ',
        order = 1,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    Boolean importAsNewDocumentationVersion = false

    @ImportParameterConfig(
        displayName = 'Import as New Branch Model Version',
        description = [
            'Should the Model/s be imported as new Branch Version/s.',
            'If selected then the latest finalised model with the same name will be chosen as the base.',
            'If not selected then the \'Model Name\' field should be used to ensure the imported Model is uniquely named,',
            'otherwise you could get an error.'],
        descriptionJoinDelimiter = ' ',
        order = 1,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    Boolean importAsNewBranchModelVersion = false

    @ImportParameterConfig(
        displayName = 'Propagate From Previous Version',
        description = 'Propagate descriptions and facets from the last version. Default: false.',
        order = 1,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        )
    )
    Boolean propagateFromPreviousVersion = false

    @ImportParameterConfig(
        optional = true,
        displayName = 'New Branch Name',
        description = [
            'Name for the branch if importing as new branch model version. Default if not provided is "main".',
            'Each branch from a finalised model must have a unique name.',
            'If the branch name already exists then the model will not be imported.'],
        descriptionJoinDelimiter = ' ',
        order = 0,
        group = @ImportGroupConfig(
            name = 'Model Branching',
            order = 1
        ))
    String newBranchName

    @ImportParameterConfig(
        displayName = 'Model Description',
        description = 'The description of the Model being imported',
        optional = true,
        order = 2,
        group = @ImportGroupConfig(
            name = 'Model Information',
            order = 2
        )
    )
    String description

    @ImportParameterConfig(
        displayName = 'Author',
        description = 'The author of the file, can be the same as the organisation',
        optional = true,
        order = 1,
        group = @ImportGroupConfig(
            name = 'Model Information',
            order = 2
        )
    )
    String author

    @ImportParameterConfig(
        displayName = 'Organisation',
        description = 'The organisation which created the Model',
        optional = true,
        order = 0,
        group = @ImportGroupConfig(
            name = 'Model Information',
            order = 2
        )
    )
    String organisation

    @ImportParameterConfig(
        hidden = true
    )
    Boolean useDefaultAuthority = true



    @ImportParameterConfig(
        displayName = 'Import Asynchronously',
        description = ['Choose to start the import process asynchronously.',
            'The import process will need to checked via the returned AsyncJob to see when its completed.',
            'Any errors which occur whilst importing can also be seen here.',
            'Default is false.'],
        descriptionJoinDelimiter = ' ',
        order = 0,
        group = @ImportGroupConfig(
            name = 'Import Process',
            order = Integer.MAX_VALUE
        ))
    Boolean asynchronous = false

}
