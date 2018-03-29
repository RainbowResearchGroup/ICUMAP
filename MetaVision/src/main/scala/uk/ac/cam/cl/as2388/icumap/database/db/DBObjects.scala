package uk.ac.cam.cl.as2388.icumap.database.db

import org.joda.time.DateTime
import scalikejdbc._
import uk.ac.cam.cl.as2388.icumap.database.{ParameterId, ParameterValue, PatientId, TextId}

case class Parameter(id: ParameterId, name: String)

case class Signal(patientId: PatientId, parameterId: ParameterId, dateTime: DateTime, value: ParameterValue)

case class FreeTextSignal(patientId: PatientId, parameterId: ParameterId, dateTime: DateTime, value: String)

case class TextSignal(patientId: PatientId, parameterId: ParameterId, dateTime: DateTime, textId: TextId,
                      value: Option[Int])

case class RangeSignal(patientId: PatientId, parameterId: ParameterId, startTime: DateTime, endTime: DateTime,
                       originalRate: Double)

case class DateTimeSignal(patientId: PatientId, parameterId: ParameterId, time: DateTime, value: DateTime)

case class EuroSCORE(patientId: PatientId, euroSCORE: Int, surgicalClass: String, priority: String,
                     surgeryList: String)

case class AdmissionInfo(patientId: PatientId, firstName: String, lastName: String,
                         hospitalNumber: String, admissionDate: Option[DateTime], dischargeDate: Option[DateTime])

case class ParameterFrequency(frequency: Long, parameterId: ParameterId, parameterName: String)

object Parameter extends SQLSyntaxSupport[Parameter] {
    def apply(rs: WrappedResultSet) = new Parameter(rs.long("ParameterID"), "")
}

object Signal extends SQLSyntaxSupport[Signal] {
    def apply(rs: WrappedResultSet): Signal = {
        new Signal(rs.long("PatientID"), rs.long("ParameterID"), rs.jodaDateTime("Time"), rs.doubleOpt("Value"))
    }
}

object FreeTextSignal extends SQLSyntaxSupport[FreeTextSignal] {
    def apply(rs: WrappedResultSet) =
        new FreeTextSignal(rs.long("PatientID"), rs.long("ParameterID"), rs.jodaDateTime("Time"), rs.string("Value"))
}

object TextSignal extends SQLSyntaxSupport[TextSignal] {
    def apply(rs: WrappedResultSet) =
        new TextSignal(rs.long("PatientID"), rs.long("ParameterID"), rs.jodaDateTime("Time"), rs.int("TextID"),
            rs.intOpt("Value"))
}

object RangeSignal extends SQLSyntaxSupport[RangeSignal] {
    def apply(rs: WrappedResultSet) =
        new RangeSignal(rs.long("PatientID"), rs.long("ParameterID"), rs.jodaDateTime("StartTime"),
            rs.jodaDateTime("EndTime"), rs.double("OriginalRate"))
}

object DateTimeSignal extends SQLSyntaxSupport[DateTime] {
    def apply(rs: WrappedResultSet): DateTimeSignal =
        new DateTimeSignal(rs.long("PatientID"), rs.long("ParameterID"), rs.jodaDateTime("Time"),
            rs.jodaDateTime("Value"))
}

object EuroSCORE extends SQLSyntaxSupport[EuroSCORE] {
    def apply(rs: WrappedResultSet) =
        new EuroSCORE(rs.long("PatientID"), rs.int("euroSCORE"), rs.string("SurgicalClass"), rs.string("Priority"),
            rs.string("SurgeryList"))
}

object AdmissionInfo extends SQLSyntaxSupport[AdmissionInfo] {
    def apply(rs: WrappedResultSet) =
        new AdmissionInfo(rs.long("PatientID"), rs.string("FirstName"), rs.string("LastName"),
            rs.string("HospitalNumber"), rs.jodaDateTimeOpt("AddmissionDate"), rs.jodaDateTimeOpt("dischargeDate"))
}

object ParameterFrequency extends SQLSyntaxSupport[ParameterFrequency] {
    def apply(rs: WrappedResultSet) =
        new ParameterFrequency(rs.long("Frequency"), rs.long("ParameterID"), rs.string("ParameterName"))
}