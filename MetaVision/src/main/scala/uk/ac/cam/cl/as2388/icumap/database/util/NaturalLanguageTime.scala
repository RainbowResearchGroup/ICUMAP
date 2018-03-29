package uk.ac.cam.cl.as2388.icumap.database.util

import scala.language.implicitConversions

class NaturalLanguageTime[A](val n: A)(implicit num: Numeric[A]) {
    def milliseconds: A = n
    def seconds: A = num.times(num.fromInt(1000), milliseconds)
    def minutes: A = num.times(num.fromInt(60),   seconds)
    def hours:   A = num.times(num.fromInt(60),   minutes)
    def days:    A = num.times(num.fromInt(24),   hours)
    def weeks:   A = num.times(num.fromInt(7),    days)
}

object NaturalLanguageTime {
    implicit def doubleToTime[A](n: A)(implicit num: Numeric[A]): NaturalLanguageTime[A] =
        new NaturalLanguageTime(n)(num)
}