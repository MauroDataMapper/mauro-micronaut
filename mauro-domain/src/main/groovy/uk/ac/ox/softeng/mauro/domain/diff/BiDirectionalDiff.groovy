package uk.ac.ox.softeng.mauro.domain.diff


import groovy.transform.CompileStatic

@CompileStatic
abstract class BiDirectionalDiff<B> extends Diff<B> {

    B right

    protected BiDirectionalDiff(Class<B> targetClass) {
        super(targetClass)
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        BiDirectionalDiff<B> diff = (BiDirectionalDiff<B>) o

        if (left != diff.left) return false
        right == diff.right
    }

    BiDirectionalDiff<B> leftHandSide(B lhs) {
        this.left = lhs
        this
    }

    BiDirectionalDiff<B> rightHandSide(B rhs) {
        this.right = rhs
        this
    }

    void setLeft(B left) {
        println("***************  setting left. ")
        this.value = left
    }

    B getLeft() {
        this.value
    }

    @Override
    int hashCode() {
        int result = super.hashCode()
        result = 31 * result + (right != null ? right.hashCode() : 0)
        result
    }
}

