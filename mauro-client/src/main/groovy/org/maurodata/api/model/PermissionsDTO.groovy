package org.maurodata.api.model

import org.maurodata.domain.security.CatalogueUser
import org.maurodata.domain.security.UserGroup

class PermissionsDTO {
    boolean readableByEveryone=false;
    boolean readableByAuthenticated=false;
    Set<UserGroup> readableByGroups=[];
    Set<UserGroup> writeableByGroups=[];
    Set<CatalogueUser> readableByUsers=[];
    Set<CatalogueUser> writeableByUsers=[];
}
