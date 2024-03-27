package uk.ac.ox.softeng.mauro.plugin

trait MauroPlugin {

    String getNamespace() {
        getClass().getPackage().getName()
    }

    String getName() {
        getClass().getSimpleName()
    }

    String version

    String displayName

}
