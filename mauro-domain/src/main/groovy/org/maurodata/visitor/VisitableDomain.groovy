package org.maurodata.visitor

interface VisitableDomain {

    <T> T accept(DomainVisitor<T> visitor)
}
