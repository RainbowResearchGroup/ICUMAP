package uk.ac.cam.cl.as2388.icumap.database.util

import scala.language.implicitConversions

class ListExtensions[A](val list: List[A]) {
    def groupByWithMap[B, C](f: A => B, g: List[A] => C): List[(B, C)] =
        list.groupBy(f).mapValues(g).toList
    
    def groupByProjected[B](f: A => B): List[List[A]] =
        list.groupBy(f).values.toList
    
    def groupByWithMapProjected[B, C](f: A => B, g: List[A] => C): List[C] =
        this.groupByWithMap(f, g).map(_._2)
}

object ListExtensions {
    implicit def listToExtendedList[A](list: List[A]): ListExtensions[A] = new ListExtensions[A](list)
}
