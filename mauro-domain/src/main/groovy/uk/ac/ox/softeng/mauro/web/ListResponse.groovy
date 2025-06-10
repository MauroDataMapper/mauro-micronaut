package uk.ac.ox.softeng.mauro.web

import uk.ac.ox.softeng.mauro.domain.terminology.Term

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import io.micronaut.data.model.Sort
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import java.lang.reflect.Field

/**
 * A ListResponse is a helper class for providing pagination in the API.  It contains a list of items, and a total count
 * of items, which is larger than or equal to the number of items returned
 */
@CompileStatic
class ListResponse<T> {

    Integer count

    @JsonInclude
    List<T> items

    /*
        If a field is using a MappedProperty, set its alias here if
        you want the search to be able to sort on that field
     */

    private static Map<String, String> mappedProperties = [:]
    static {
        mappedProperties.put("idx", "order");
    }

    static ListResponse from(List items) {
        new ListResponse(count: items.size(), items: items ?: [])
    }

    static ListResponse from(List items, PaginationParams params) {

        if (params == null) {
            return new ListResponse(count: items.size(), items: items ?: [])
        }

        List filteredItems = filter(items, params)

        return new ListResponse(count: filteredItems.size(), items: apply(filteredItems, params))
    }

    void bindItems(ObjectMapper objectMapper, Class<T> clazz) {
        items = items.collect {item ->
            return objectMapper.convertValue(item, clazz)
        }
    }

    List<T> each(@ClosureParams(value = FromString, options = ["T"]) Closure c) {
        items?.each c
    }

    T find(@ClosureParams(value = FromString, options = ["T"]) Closure c) {
        items?.find c
    }

    private static <T> List<T> filter(List<T> input, PaginationParams params) {
        if (input == null || input.isEmpty()) {
            return []
        }
        if (params == null) {
            return input
        }

        if (params.label || params.description || params.code || params.definition) {
            if (input.get(0) instanceof AdministeredItem) {
                List<AdministeredItem> filterable = (List<AdministeredItem>) input

                if (params.label) {
                    filterable = filterable.findAll {it.label?.containsIgnoreCase(params.label)}
                }

                if (params.description) {
                    filterable = filterable.findAll {it.description?.containsIgnoreCase(params.description)}
                }

                if (params.code) {
                    filterable = filterable.findAll {it instanceof Term && ((Term) it).code?.containsIgnoreCase(params.code)}
                }

                if (params.definition) {
                    filterable = filterable.findAll {it instanceof Term && ((Term) it).definition?.containsIgnoreCase(params.definition)}
                }

                return (List<T>) filterable
            }
        }

        return input
    }

    private static <T> List<T> apply(List<T> input, PaginationParams params) {
        if (input == null || input.isEmpty()) {return []}
        if (params == null) {return input}

        Class<T> clazz = (Class<T>) input.get(0).getClass()

        // Sort
        Sort sort = toSort(params)

        Comparator<T> comparator = buildComparator(sort, clazz)

        List<T> sorted
        if (comparator == null) {
            // Couldn't resolve the sort field to use
            sorted = input
            System.err.println("Warning: unable to sort")
        } else {
            sorted = input.toSorted(comparator)
        }

        // Paginate
        int start = Math.max(0, params.offset ?: 0)
        int end = Math.min(start + (params.max ?: 50), sorted.size())
        return sorted.subList(start, end)
    }

    private static Sort toSort(PaginationParams params) {
        Sort.Order.Direction direction = params.order?.toLowerCase() == "desc" ? Sort.Order.Direction.DESC : Sort.Order.Direction.ASC
        String sortField = params.sort ? params.sort : "label"

        Sort sortObj = Sort.of([new Sort.Order(sortField, direction, true)])

        return sortObj
    }

    private static <T> Comparator<T> buildComparator(Sort sort, Class<T> clazz) {
        Comparator<T> comparator = null

        sort.orderBy.each {Sort.Order order ->
            Comparator<T> next

            try {
                String sortProp = order.property
                final String mappedName = mappedProperties.get(sortProp)
                if (mappedName != null) {sortProp = mappedName}

                next = Comparator.comparing(
                    {T obj ->

                        Field field = findFieldInHierarchy(obj.getClass(), sortProp)
                        if (field == null) {
                            return null
                        }
                        field.setAccessible(true)
                        return field.get(obj) as Comparable
                    },
                    Comparator.nullsFirst(Comparator.naturalOrder())
                )

                if (order.direction == Sort.Order.Direction.DESC) {
                    next = next.reversed()
                }

                comparator = comparator == null ? next : comparator.thenComparing(next)

            } catch (Throwable e) {
                // Graceful fallback — you can log or ignore
                System.err.println("Warning: unable to sort by '${order.property}': ${e.message}")
            }
        }

        return comparator
    }

    private static Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
        Class<?> current = clazz
        while (current != null && current != Object) {
            try {
                return current.getDeclaredField(fieldName)
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass()
            }
        }
        return null
    }

}