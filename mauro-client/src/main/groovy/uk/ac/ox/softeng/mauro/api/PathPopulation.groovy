package uk.ac.ox.softeng.mauro.api

class PathPopulation {

    static String populatePath(final String self, Map<String, Object> replacements) {
        String populatedPath = self
        replacements.each {key, value ->
            populatedPath = populatedPath.replace("{$key}", value.toString())
            populatedPath = populatedPath.replace("{/$key}", value.toString())
        }
        if(populatedPath.find(/\{[^\}]+\}/)) {
            throw new Exception("Unpopulated path parameters: $populatedPath")
        }
        populatedPath
    }



}
