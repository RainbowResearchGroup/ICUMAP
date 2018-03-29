package uk.ac.cam.cl.as2388.icumap.database.db

import com.microsoft.sqlserver.jdbc.SQLServerDriver
import scalikejdbc._
import scalikejdbc.config.DBs
import uk.ac.cam.cl.as2388.icumap.database.{ParameterId, PatientId}

/**
  * @param instance Name of SQLServer to connect to
  * @param database Name of database to connect to
  * @param port     Port on which the SQLServer is running.
  */
case class ConnectionDescription(instance: String, database: String, port: Int)

/**
  * @param connectionString Address of server/database to connect to, and login information.
  */
class MVDatabase(connectionString: String) {
    
    /** A convenience constructor for creating connection strings with Windows account authentication. */
    def this(connectionDescription: ConnectionDescription) {
        this(s"jdbc:sqlserver://localhost:${connectionDescription.port};" +
            s"instance=${connectionDescription.instance};" +
            s"database=${connectionDescription.database};" +
            "integratedSecurity=true;" +
            "loginTimeout=2;")
    }
    
    new SQLServerDriver() // Prevent SQLServerDriver import from being inadvertently excluded by the optimising compiler
    ConnectionPool.singleton(connectionString, "", "")
    DBs.setupAll()
    implicit val Session: AutoSession.type = AutoSession

    def patientIdsInDatabase(): List[ParameterId] =
        sql"""
              SELECT DISTINCT PatientID FROM Patients
          """.map(rs => rs.long("PatientID")).list.apply()
    
    def numericSignals(patientId: PatientId, parameterId: ParameterId): List[Signal] =
        sql"""
            SELECT Signals.PatientID, Signals.ParameterID, Signals.Time,
                Parameters.ParameterName, Signals.Value, Signals.Comments
            FROM Signals JOIN Parameters ON Parameters.ParameterID=Signals.ParameterID
            WHERE Parameters.ParameterID=$parameterId AND FreeTextSignals.PatientID=$patientId
            ORDER BY Signals.PatientID, Signals.Time
        """.map(rs => Signal(rs)).list.apply()
    
    def numericSignals(minPatient: PatientId, maxPatient: PatientId, parameters: List[ParameterId]): List[Signal] =
        sql"""
            SELECT Signals.PatientID, Signals.ParameterID, Signals.Time,
                Parameters.ParameterName, Signals.Value, Signals.Comments
            FROM Signals JOIN Parameters ON Parameters.ParameterID=Signals.ParameterID
            WHERE Parameters.ParameterID IN ($parameters)
                AND PatientID >= $minPatient AND PatientID < $maxPatient
            ORDER BY Signals.PatientID, Signals.Time
        """.map(rs => Signal(rs)).list.apply()
    
    def unitConvertedSignals(minPatient: PatientId, maxPatient: PatientId,
                             parameters: List[ParameterId]): List[Signal] =
        sql"""
            SELECT Signals.PatientID, Signals.ParameterID, Signals.Time,
                Parameters.ParameterName, Signals.Comments,
                (Signals.Value - Units.Addition) / Units.Multiplier AS Value
            FROM Signals
                JOIN Parameters ON Parameters.ParameterID=Signals.ParameterID
                JOIN Units ON Parameters.UnitID=Units.UnitID
            WHERE Parameters.ParameterID IN ($parameters)
                AND PatientID >= $minPatient AND PatientID < $maxPatient
            ORDER BY Signals.PatientID, Signals.Time
        """.map(rs => Signal(rs)).list.apply()
    
    def unitConvertedValidatedSignals(patient: PatientId, parameter: ParameterId): List[Signal] =
        sql"""
            SELECT Signals.PatientID, Signals.ParameterID, Signals.Time,
                Parameters.ParameterName, Signals.Comments,
                (Signals.Value - Units.Addition) / Units.Multiplier AS Value
            FROM Signals
                JOIN Parameters ON Parameters.ParameterID=Signals.ParameterID
                JOIN Units ON Parameters.UnitID=Units.UnitID
            WHERE Parameters.ParameterID = $parameter
                AND PatientID = $patient
                AND ValidationUserID IS NOT NULL AND Warning=0 AND Error=0
            ORDER BY Signals.Time
        """.map(rs => Signal(rs)).list.apply()
    
    def freeTextSignals(patientId: PatientId, parameterId: ParameterId): List[FreeTextSignal] =
       sql"""
            SELECT FreeTextSignals.PatientID, Parameters.ParameterID, FreeTextSignals.Time,
                Parameters.ParameterName, FreeTextSignals.Value, FreeTextSignals.Comments
            FROM FreeTextSignals JOIN Parameters ON Parameters.ParameterID=FreeTextSignals.ParameterID
            WHERE Parameters.ParameterID=$parameterId AND FreeTextSignals.PatientID=$patientId
            ORDER BY FreeTextSignals.PatientID, FreeTextSignals.Time
        """.map(rs => FreeTextSignal(rs)).list.apply()

    def freeTextSignals(patientId: PatientId): List[FreeTextSignal] =
        sql"""
            SELECT FreeTextSignals.PatientID, FreeTextSignals.ParameterID, FreeTextSignals.Time,
                FreeTextSignals.Value
            FROM FreeTextSignals
            WHERE PatientID=$patientId
        """.map(rs => FreeTextSignal(rs)).list.apply()

    def freeTextSignals(patientId: PatientId, parameters: List[ParameterId]): List[FreeTextSignal] =
        sql"""
            SELECT FreeTextSignals.PatientID, FreeTextSignals.ParameterID, FreeTextSignals.Time,
                FreeTextSignals.Value
            FROM FreeTextSignals
            WHERE ParameterID IN $parameters AND PatientID = $patientId
        """.map(rs => FreeTextSignal(rs)).list.apply()
    
    def freeTextSignals(minPatient: PatientId, maxPatient: PatientId,
                        parameters: List[ParameterId]): List[FreeTextSignal] =
        sql"""
            SELECT FreeTextSignals.PatientID, FreeTextSignals.ParameterID, FreeTextSignals.Time,
                FreeTextSignals.VALUE
            FROM FreeTextSignals
            WHERE ParameterID IN ($parameters) AND PatientID >= $minPatient AND PatientID < $maxPatient
        """.map(rs => FreeTextSignal(rs)).list.apply()
    
    def dateTimeSignals(minPatient: PatientId, maxPatient: PatientId,
                        parameters: List[ParameterId]): List[DateTimeSignal] =
        sql"""
            SELECT DateTimeSignals.PatientID, DateTimeSignals.ParameterID, DateTimeSignals.Time,
                DateTimeSignals.Value
            FROM DateTimeSignals
            WHERE ParameterID IN ($parameters) AND PatientID >= $minPatient AND PatientID < $maxPatient
        """.map(rs => DateTimeSignal(rs)).list.apply()
    
    def euroSCOREs(minPatient: PatientId, maxPatient: PatientId): List[EuroSCORE] =
        sql"""
            SELECT euroSCORE.euroSCORE, euroSCORE.SurgicalClass, euroSCORE.Priority,
                euroSCORE.SurgeryList, Patients.PatientID
            FROM euroSCORE JOIN Patients ON euroSCORE.CCAAdmissionDate=Patients.AddmissionDate
                AND euroSCORE.CCADischargeDate=Patients.DischargeDate
            WHERE Patients.PatientID >= $minPatient AND Patients.PatientID < $maxPatient
        """.map(rs => EuroSCORE(rs)).list.apply()

    def textSignals(patientId: PatientId, parameterId: ParameterId): List[TextSignal] =
        sql"""
             SELECT TextSignals.PatientID, TextSignals.ParameterID, TextSignals.Time,
                TextSignals.TextID, ParametersText.Value
             FROM TextSignals JOIN ParametersText
                ON TextSignals.ParameterID=ParametersText.ParameterID
                AND TextSignals.TextID=ParametersText.TextID
             WHERE TextSignals.ParameterID = $parameterId AND TextSignals.PatientID = $patientId
                AND ValidationUserID IS NOT NULL AND Warning=0 AND Error=0
        """.map(rs => TextSignal(rs)).list.apply()
    
    def textSignals(minPatient: PatientId, maxPatient: PatientId,
                    parameters: List[ParameterId]): List[TextSignal] =
        sql"""
             SELECT TextSignals.PatientID, TextSignals.ParameterID, TextSignals.Time,
                TextSignals.TextID, ParametersText.Value
             FROM TextSignals JOIN ParametersText
                ON TextSignals.ParameterID=ParametersText.ParameterID
                AND TextSignals.TextID=ParametersText.TextID
             WHERE TextSignals.ParameterID IN ($parameters) AND
                PatientID >= $minPatient AND PatientID < $maxPatient
        """.map(rs => TextSignal(rs)).list.apply()
    
    def textSignalsAsFreeText(minPatient: PatientId, maxPatient: PatientId,
                              parameters: List[ParameterId]): List[FreeTextSignal] =
        sql"""
            SELECT TextSignals.PatientID, TextSignals.ParameterID, TextSignals.Time,
                ParametersText.Text AS Value
            FROM TextSignals JOIN ParametersText ON TextSignals.TextID=ParametersText.TextID
                AND TextSignals.ParameterID=ParametersText.ParameterID
            WHERE TextSignals.ParameterID IN ($parameters)
                AND TextSignals.PatientID >= $minPatient AND TextSignals.PatientID < $maxPatient
        """.map(rs => FreeTextSignal(rs)).list.apply()
    
    def textSignalsAsMap(minPatient: PatientId, maxPatient: PatientId,
                         parameter: ParameterId): Map[PatientId, List[Int]] = {
        sql"""
            SELECT DISTINCT PatientID, TextID
            FROM TextSignals
            WHERE ParameterID=$parameter AND PatientID >= $minPatient AND PatientID < $maxPatient
        """
            .map(rs => (rs.long("PatientID"), rs.int("TextID")))
            .list
            .apply()
            .groupBy(_._1)
            .mapValues(_.map(_._2))
    }
    
    def signalsForParameters(parameters: List[ParameterId]): List[Signal] = {
        sql"""
            SELECT Signals.PatientID, Signals.ParameterID, Signals.Time, Signals.Value
            FROM Signals
            WHERE Signals.ParameterID IN ($parameters) AND Signals.PatientID > 25000
        """.map(rs => Signal(rs)).list.apply()
    }

    def rangeSignals(patient: PatientId, parameter: ParameterId): List[RangeSignal] = {
        sql"""
            SELECT RangeSignals.PatientID, RangeSignals.ParameterID, RangeSignals.StartTime, RangeSignals.EndTime,
                RangeSignals.OriginalRate
            FROM RangeSignals
            WHERE RangeSignals.ParameterID=$parameter AND RangeSignals.PatientID=$patient
          """.map(rs => RangeSignal(rs)).list.apply()
    }
    
    def parameters(): List[Parameter] =
        sql"""
             SELECT ParameterID, ParameterName
             FROM Parameters
             ORDER BY ParameterID
            """.map(rs => Parameter(rs)).list.apply()

    def admissions(): List[AdmissionInfo] =
        sql"""
              SELECT PatientID, FirstName, LastName, HospitalNumber, AddmissionDate, DischargeDate
              FROM Patients WHERE PatientID > 0
            """.map(rs => AdmissionInfo(rs)).list.apply()
    
    def admissions(minPatient: PatientId, maxPatient: PatientId): List[AdmissionInfo] =
        sql"""
            SELECT DISTINCT Patients.PatientID, FirstName, LastName, HospitalNumber, AddmissionDate, DischargeDate
            FROM Patients
            WHERE Patients.PatientID >= $minPatient AND Patients.PatientID < $maxPatient
        """.map(rs => AdmissionInfo(rs)).list.apply()
    
    def computePatientParameterFrequencies(): List[ParameterFrequency] =
        sql"""
            SELECT COUNT(DISTINCT Signals.PatientID) AS Frequency, Signals.ParameterID, ParameterName
            FROM Signals JOIN Parameters ON Signals.ParameterID=Parameters.ParameterID
            JOIN Patients ON Patients.PatientID=Signals.PatientID
            WHERE Signals.Time < DATEADD(HH, 4, AddmissionDate)
            GROUP BY Signals.ParameterID, ParameterName
            ORDER BY Frequency Desc
            """.map(rs => ParameterFrequency(rs)).list.apply()
    
    def extractEuroSCORE(): Map[(Long, Long), Int] = {
        sql"""
            SELECT CCAAdmissionDate as AdmissionDate, CCADischargeDate as DischargeDate, euroSCORE
            FROM euroSCORE
        """
            .map(rs =>
                ((rs.timestamp("AdmissionDate").getTime, rs.timestamp("DischargeDate").getTime), rs.int("euroSCORE"))
            )
            .list.apply().toMap
    }
}

case class Admissions(db: MVDatabase) {
    private lazy val _admissions: Map[PatientId, AdmissionInfo] =
        db.admissions().map(x => (x.patientId, x)).toMap
    
    def apply(patientId: PatientId): AdmissionInfo = _admissions(patientId)
}