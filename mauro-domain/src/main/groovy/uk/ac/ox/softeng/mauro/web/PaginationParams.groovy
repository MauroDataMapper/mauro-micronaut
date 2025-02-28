package uk.ac.ox.softeng.mauro.web
import io.micronaut.core.annotation.Introspected

@Introspected
class PaginationParams {
    Integer offset = 0
    Integer max = 50
    String sort = "id"
    String order = "asc"
    String label = null
    String description = null
}
