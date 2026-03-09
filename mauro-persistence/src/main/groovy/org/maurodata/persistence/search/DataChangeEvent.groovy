package org.maurodata.persistence.search

record DataChangeEvent(Class<?> repoClass, String methodName, Object[] args) {
}