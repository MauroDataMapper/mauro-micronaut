package org.maurodata.api.model

class MergeDiffDTO {

    /*
    sourceId	"12ee3883-b794-4655-9440-c46e0e6a4e4a"
targetId	"53ff1433-5bdd-440f-bf36-a7dcb022523c"
path	"dm:Model Version Tree DataModel$testyBranch"
label	"Model Version Tree DataModel"
count	4
diffs:
     */

    UUID sourceId
    UUID targetId
    String path
    String label
    Integer count

    Integer getCount() {
        if (diffs == null) {count = 0; return count}
        count = diffs.size()
        return count
    }

    private void setCount(Integer count) {
        // Nope
    }
    List<MergeFieldDiffDTO> diffs
}
