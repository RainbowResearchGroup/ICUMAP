package uk.ac.cam.cl.as2388.icumap.database.extractors

import java.io.{File, PrintWriter}

import org.joda.time.{DateTime, Period}
import play.api.libs.json._
import uk.ac.cam.cl.as2388.icumap.database.db._
import uk.ac.cam.cl.as2388.icumap.database.extractors.PatientAttributes.{Attribute, AttributeValue}
import uk.ac.cam.cl.as2388.icumap.database.util.ListExtensions.listToExtendedList
import uk.ac.cam.cl.as2388.icumap.database.util.MapExtensions.mapToExtendedMap
import uk.ac.cam.cl.as2388.icumap.database.{ParameterId, PatientId}

case class PatientAttributes(patientId: PatientId, attributes: Map[Attribute, AttributeValue]) {
    def merge(that: PatientAttributes): PatientAttributes =
        PatientAttributes(this.patientId, this.attributes ++ that.attributes)
}

object PatientAttributes {
    type Attribute = String
    type AttributeValue = Any
    
    implicit val patientAttributesWrites = new Writes[PatientAttributes] {
        override def writes(patientAttributes: PatientAttributes): JsValue = Json.obj(
            "patientId" -> patientAttributes.patientId,
            "attributes" -> patientAttributes.attributes.map({
                case (a, b: String) =>                  (a.toString, Json.toJson(b))
                case (a, b: List[String] @unchecked) => (a.toString, Json.toJson(b))
                case (a, b: Boolean) =>                 (a.toString, Json.toJson(b))
                case (a, b) =>                          (a.toString, JsString(b.toString))
            })
        )
    }
    
    def run(attributesOutputPath: String, minId: PatientId, maxId: PatientId,
            freeTextAttributes: List[ParameterId], textAttributes: List[ParameterId],
            attributesToStrip: Set[String], stripAttributesFromPatient: PatientId => Boolean,
            customExtractors: List[(PatientId, PatientId) => List[PatientAttributes]],
            db: MVDatabase): Unit = {
        println("Exporting patient attributes...")
        val patientAttributes: List[PatientAttributes] =
            patientAttributesFromSignals(db, minId, maxId, freeTextAttributes ::: textAttributes) :::
            patientAttributesFromAdmission(db, minId, maxId) :::
            customExtractors.flatMap(_(minId, maxId))
    
        println("Extracted attributes; processing...")
        val mergedPatientAttributes = patientAttributes.groupByWithMapProjected(_.patientId, _.reduce(_ merge _))
        val dataForExport = stripAttributesFromPatients(mergedPatientAttributes, stripAttributesFromPatient, attributesToStrip)
        
        println("Writing json")
        exportToJson(dataForExport, attributesOutputPath)
        
        println(s"Attributes for ${dataForExport.length} patients exported.")
    }
    
    private def patientAttributesFromSignals(db: MVDatabase, minPatient: PatientId, maxPatient: PatientId,
                                             parameters: List[ParameterId]): List[PatientAttributes] = {
        val rawSignals =
            db.freeTextSignals(minPatient, maxPatient, parameters) :::
            db.textSignalsAsFreeText(minPatient, maxPatient, parameters)
    
        // Take only the most recent free text entry for each (patient id, parameter id) pair
        val mostRecentSignals: List[((PatientId, ParameterId), String)] = rawSignals.groupByWithMap(
            p => (p.patientId, p.parameterId),
            l => l.maxBy(_.dateTime.toInstant.getMillis).value
        )
    
        // Convert from List[(PatientId, ParameterId), String)] to List[(PatientId, Map[ParameterId, String])]
        val groupedByPatient: List[(PatientId, Map[String, String])] = mostRecentSignals
            .map({case ((patientId, parameterId), value) => (patientId, (parameterId, value))})
            .groupByWithMap(_._1, l => l.map(_._2).toMap.mapKeys(_.toString))
    
        // Convert this to a list of serializable case classes
        groupedByPatient.map({case (patientId, attributes) =>
            PatientAttributes(patientId, attributes)
        })
    }
    
    private def patientAttributesFromAdmission(db: MVDatabase, minPatient: PatientId,
                                               maxPatient: PatientId): List[PatientAttributes] = {
        def dateToMap(label: String, date: Option[DateTime]): Map[Attribute, AttributeValue] = date match {
            case Some(d) => Map(label -> d.toInstant.getMillis.toString)
            case None => Map()
        }
        
        db.admissions(minPatient, maxPatient).map(admission =>
            PatientAttributes(admission.patientId,
                dateToMap("admissionDate", admission.admissionDate) ++
                dateToMap("dischargeDate", admission.dischargeDate)
            )
        )
    }
    
    def attributesFromMortality(db: MVDatabase, dischargeParameter: ParameterId,
                                dischargeId: Int): (PatientId, PatientId) => List[PatientAttributes] = {
        (minPatient: PatientId, maxPatient: PatientId) => db
            .textSignalsAsMap(minPatient, maxPatient, dischargeParameter)
            .map({case (patientId, destinations) =>
                PatientAttributes(patientId, Map("died" -> destinations.contains(dischargeId)))
            })
            .toList
    }
    
    def attributesFromEuroSCORE(db: MVDatabase): (PatientId, PatientId) => List[PatientAttributes] = {
        (minPatient: PatientId, maxPatient: PatientId) => {
            def explodeSurgeryList(surgeryList: String): List[String] =
                surgeryList /*.toUpperCase()*/ .split("; ").map(_.trim).filter(_ != "(REDO)").toList
    
            def isRedo(surgeryList: String): Boolean = surgeryList.contains("(REDO)")
    
            db.euroSCOREs(minPatient, maxPatient).map(euroSCORE =>
                PatientAttributes(euroSCORE.patientId, Map(
                    "euroSCORE" -> euroSCORE.euroSCORE.toString,
                    "euroSCOREClass" -> euroSCORE.surgicalClass,
                    "euroSCOREPriority" -> euroSCORE.priority,
                    "euroSCORESurgeryList" -> explodeSurgeryList(euroSCORE.surgeryList),
                    "euroSCORESurgeryIsRedo" -> isRedo(euroSCORE.surgeryList)
                ))
            )
        }
    }
    
    def attributesFromAge(db: MVDatabase,
                          DOBParameter: ParameterId): (PatientId, PatientId) => List[PatientAttributes] = {
        (minPatient: PatientId, maxPatient: PatientId) => {
            val admissionDates: Map[PatientId, Option[DateTime]] =
                db.admissions(minPatient, maxPatient).map(p => (p.patientId, p.admissionDate)).toMap
    
            def ageForPatient(p: DateTimeSignal) =
                admissionDates(p.patientId).map(admissionDate => new Period(p.value, admissionDate).getYears)
    
            db
                .dateTimeSignals(minPatient, maxPatient, DOBParameter :: Nil)
                .map(p => (p.patientId, ageForPatient(p)))
                .flatMap(p => p._2 match {
                    case Some(age) => Some(PatientAttributes(p._1, Map("age" -> age)))
                    case None => None
                })
        }
    }
    
    def attributesFromICM(db: MVDatabase,
                          icmParameter: ParameterId): (PatientId, PatientId) => List[PatientAttributes] = {
        (minPatient: PatientId, maxPatient: PatientId) => {
            db
                .freeTextSignals(minPatient, maxPatient, List(icmParameter))
                .map(p => (p.patientId, ICM.parse(p.value)))
                .filter(_._2.isDefined)
                .map(p => (p._1, p._2.get.toConditionString))
                .filter(_._2.isDefined)
                .map(p => PatientAttributes(p._1, Map("condition" -> p._2.get)))
        }
    }
    
    private def stripAttributesFromPatients(patientAttributes: List[PatientAttributes],
                                            stripPatient: PatientId => Boolean,
                                            attributesToStrip: Set[String]): List[PatientAttributes] = {
        patientAttributes.map(p => stripPatient(p.patientId) match {
            case true => PatientAttributes(p.patientId, p.attributes.filterKeys(!attributesToStrip.contains(_)))
            case false => p
        })
    }
    
    private def exportToJson(data: List[PatientAttributes], filePath: String): Unit = {
        println(filePath)
        val file = new File(filePath)
        file.getParentFile.mkdirs
        
        val json = Json.toJson(data).toString
        Some(new PrintWriter(file))
            .foreach {p => p.write(json); p.close()}
    }
}
