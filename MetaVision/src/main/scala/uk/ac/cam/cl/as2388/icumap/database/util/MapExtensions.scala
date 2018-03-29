package uk.ac.cam.cl.as2388.icumap.database.util

import scala.language.implicitConversions

class MapExtensions[K, V](val map: Map[K, V]) {
    def mapKeys[L](f: K => L): Map[L, V] = map.map({case (k, v) => (f(k), v)})
}

object MapExtensions {
    implicit def mapToExtendedMap[K, V](map: Map[K, V]): MapExtensions[K, V] = new MapExtensions[K, V](map)
}
