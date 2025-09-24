package org.maurodata.domain.model

import groovy.transform.CompileStatic

@CompileStatic
class ItemUtils {

    static <T extends Item> List<T> copyItems(List<T> fromItemList, List<T> toItemList) {
        if(fromItemList == null){return toItemList}

        if(toItemList == null){
            return [] + fromItemList
        }

        if(toItemList.is(fromItemList)){return toItemList}

        if(toItemList.isEmpty()){
            return toItemList + fromItemList
        }

        // Now we have to do a dupe check
        List<T> back = [] + toItemList
        fromItemList.forEach{ T fromItem ->

            T found=back.find{T match -> match.is(fromItem)}
            if(found == null){
                found=back.find{T match -> match.id == fromItem.id}
            }

            if(found!=null){
                int pos=back.indexOf(found)
                back.set(pos,fromItem)
            }
            else
            {
                back.add(fromItem)
            }
        }
        return back
    }

    static <T extends Item> Set<T> copyItems(Set<T> fromItemList, Set<T> toItemList) {
        if(fromItemList == null){return toItemList}

        if(toItemList == null){
            return [] as Set + fromItemList
        }

        if(toItemList.is(fromItemList)){return toItemList}

        if(toItemList.isEmpty()){
            return toItemList + fromItemList
        }

        // Now we have to do a dupe check
        Set<T> back = [] as Set + toItemList
        fromItemList.forEach{ T fromItem ->

            T found=back.find{T match -> match.is(fromItem)}
            if(found == null){
                found=back.find{T match -> match.id == fromItem.id}
            }

            if(found!=null){
                back.remove(found)
                back.add(fromItem)
            }
            else
            {
                back.add(fromItem)
            }
        }
        return back
    }

    static <T extends Item> Collection<T> copyItems(Collection<T> fromItemList, Collection<T> toItemList) {
        if(fromItemList == null){return toItemList}

        if(toItemList == null){
            return [] + fromItemList
        }

        if(toItemList.is(fromItemList)){return toItemList}

        if(toItemList.isEmpty()){
            return toItemList + fromItemList
        }

        // Now we have to do a dupe check
        Collection<T> back = [] + toItemList
        fromItemList.forEach{ T fromItem ->

            T found=back.find{T match -> match.is(fromItem)}
            if(found == null){
                found=back.find{T match -> match.id == fromItem.id}
            }

            if(found!=null){
                int pos=back.indexOf(found)
                back.set(pos,fromItem)
            }
            else
            {
                back.add(fromItem)
            }
        }
        return back
    }

    static <T extends Object> T copyItem(T from, T to){
        if(from == null){return to}
        return from
    }

    static <T extends String> T copyItem(T from, T to) {
        if (from == null) {return to}
        if (from.isEmpty()) {return to}
        return from
    }
}