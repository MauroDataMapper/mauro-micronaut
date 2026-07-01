CREATE INDEX IF NOT EXISTS data_type_model_resource_idx
    ON datamodel.data_type (model_resource_domain_type, model_resource_id)
    WHERE model_resource_id IS NOT NULL;
