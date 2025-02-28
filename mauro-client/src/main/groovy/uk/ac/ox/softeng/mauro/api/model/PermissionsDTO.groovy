package uk.ac.ox.softeng.mauro.api.model

import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.domain.security.UserGroup

class PermissionsDTO {
    boolean readableByEveryone=false;
    boolean readableByAuthenticated=false;
    Set<UserGroup> readableByGroups=[];
    Set<UserGroup> writeableByGroups=[];
    Set<CatalogueUser> readableByUsers=[];
    Set<CatalogueUser> writeableByUsers=[];
}
