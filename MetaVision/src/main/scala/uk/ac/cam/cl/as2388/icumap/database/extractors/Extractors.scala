package uk.ac.cam.cl.as2388.icumap.database.extractors

import org.joda.time.{DateTime, Instant}
import uk.ac.cam.cl.as2388.icumap.database.db._
import uk.ac.cam.cl.as2388.icumap.database.extractors.FeaturesExtractor.Parameter
import uk.ac.cam.cl.as2388.icumap.database.{ParameterId, PatientId, TextId}

abstract class Extractor {
    def extract(patientId: PatientId, timeStep: Int): Option[Double]
}

class ExtractorFactory(db: MVDatabase) {
    def generate(parameter: Parameter, timeStepSize: Instant): Option[Extractor] =
        (parameter.dataType, parameter.argument) match {
            case (Some("sensor"), _) =>          Some(new NumericExtractor(timeStepSize, parameter.parameterId.get))
            case (Some("ordinal"), Some("constant")) =>
                                                 Some(new TimeInvariantOrdinalExtractor(parameter.parameterId.get))
            case (Some("ordinal"), _) =>         Some(new OrdinalExtractor(timeStepSize, parameter.parameterId.get))
            case (Some("cardinal"), Some("constant")) =>
                                                 Some(new TimeInvariantCardinalExtractor(parameter.parameterId.get))
            case (Some("cardinal"), _) =>        Some(new CardinalExtractor(timeStepSize, parameter.parameterId.get))
            case (Some("drug"), _) =>            Some(new DrugsExtractor(timeStepSize, parameter.parameterId.get))
            case (Some("died"), Some(diedId)) => Some(new DiedExtractor(parameter.parameterId.get, diedId.toInt))
            case (Some("icm-type"), _) =>        Some(new ICMExtractor(ICMProperty.Type, parameter.parameterId.get))
            case (Some("icm-system"), _) =>      Some(new ICMExtractor(ICMProperty.System, parameter.parameterId.get))
            case (Some("icm-site"), _) =>        Some(new ICMExtractor(ICMProperty.Site, parameter.parameterId.get))
            case (Some("icm-process"), _) =>     Some(new ICMExtractor(ICMProperty.Process, parameter.parameterId.get))
            case (Some("icm-condition"), _) =>   Some(new ICMExtractor(ICMProperty.Condition, parameter.parameterId.get))
            case (Some("patient-id"), _) =>      Some(new PatientIdExtractor)
            case (Some("time-step"), _) =>       Some(new TimeStepExtractor)
            case _ =>
                println(s"Warning: No extractor found for parameter: $parameter")
                None
        }
    
    val admissions = Admissions(db)
    
    def timeStepFor(patient: PatientId, dateTime: DateTime, timeStepSize: Instant): Int = {
        val admissionTime = admissions(patient).admissionDate.get.getMillis
        val queryTime = dateTime.getMillis
        val windowSizeTime = timeStepSize.getMillis
        
        ((queryTime - admissionTime) / windowSizeTime).toInt
    }
    
    abstract class CachingTimeSeriesExtractor extends Extractor {
        private var cachedPatientId: Option[PatientId] = None
        type TimeStep = Int
        private var cachedData: Option[Map[TimeStep, Double]] = None
        
        override def extract(patientId: PatientId, timeStep: Int): Option[Double] = {
            if (!cachedPatientId.contains(patientId)) {
                cachedPatientId = Some(patientId)
                cachedData = Some(loadPatient(patientId))
            }
            cachedData.get.get(timeStep)
        }
        
        def loadPatient(patientId: PatientId): Map[Int, Double]
    }
    
    class NumericExtractor(timeStepSize: Instant, parameterId: ParameterId) extends CachingTimeSeriesExtractor {
        override def loadPatient(patientId: PatientId): Map[Int, Double] =
            db.unitConvertedValidatedSignals(patientId, parameterId)
                .filter(_.value.isDefined)
                .groupBy(signal => timeStepFor(patientId, signal.dateTime, timeStepSize))
                .mapValues(l => l.length match {
                    case 1 => l.head.value.get
                    case _ =>
                        val sorted = l.map(_.value.get).sorted
                        (sorted(l.length / 2) + sorted(l.length - l.length / 2)) / 2
                })
    }
    
    class OrdinalExtractor(timeStepSize: Instant, parameterId: ParameterId) extends CachingTimeSeriesExtractor {
        override def loadPatient(patientId: PatientId): Map[Int, Double] =
            db.textSignals(patientId, parameterId)
                .filter(_.value.isDefined)
                .groupBy(signal => timeStepFor(patientId, signal.dateTime, timeStepSize))
                .mapValues(l => l.maxBy(_.dateTime.getMillis).value.get)
        
    }
    
    class CardinalExtractor(timeStepSize: Instant, parameterId: ParameterId) extends CachingTimeSeriesExtractor {
        override def loadPatient(patientId: PatientId): Map[Int, Double] =
            db.textSignals(patientId, parameterId)
                .groupBy(signal => timeStepFor(patientId, signal.dateTime, timeStepSize))
                .mapValues(l => l.maxBy(_.dateTime.getMillis).textId)
    }
    
    class DrugsExtractor(timeStepSize: Instant, parameterId: ParameterId) extends Extractor {
        private var cachedPatientId: Option[PatientId] = None
        private var timeStepsAdministered = Set[Int]()
        
        override def extract(patientId: PatientId, timeStep: Int): Option[Double] = {
            if (!cachedPatientId.contains(patientId)) {
                cachedPatientId = Some(patientId)
                timeStepsAdministered = Set()
                
                val signals = db.rangeSignals(patientId, parameterId)
                signals.foreach(signal => {
                    val startStep = timeStepFor(patientId, signal.startTime, timeStepSize)
                    val endStep = timeStepFor(patientId, signal.endTime, timeStepSize)
                    for (step <- startStep to endStep) {
                        timeStepsAdministered += step
                    }
                })
            }
            
            timeStepsAdministered.contains(timeStep) match {
                case true => Some(1.0)
                case false => Some(0.0)
            }
        }
    }
    
    abstract class CachingTimeInvariantExtractor extends Extractor {
        private var cachedPatientId: Option[PatientId] = None
        private var cachedData: Option[Double] = None
        
        override def extract(patientId: PatientId, timeStep: Int): Option[Double] = {
            if (!cachedPatientId.contains(patientId)) {
                cachedPatientId = Some(patientId)
                cachedData = loadPatient(patientId)
            }
            cachedData
        }
        
        def loadPatient(patientId: PatientId): Option[Double]
    }
    
    class TimeInvariantOrdinalExtractor(parameterId: ParameterId) extends CachingTimeInvariantExtractor {
        override def loadPatient(patientId: PatientId): Option[Double] = {
            db.textSignals(patientId, parameterId) match {
                case Nil => None
                case l => Some(l.filter(_.value.isDefined).maxBy(_.dateTime.getMillis).value.get)
            }
        }
    }
    
    class TimeInvariantCardinalExtractor(parameterId: ParameterId) extends CachingTimeInvariantExtractor {
        override def loadPatient(patientId: PatientId): Option[Double] = {
            db.textSignals(patientId, parameterId) match {
                case Nil => Some(0.0)
                case l => Some(l.maxBy(_.dateTime.getMillis).textId.toDouble)
            }
        }
    }
    
    class DiedExtractor(parameterId: ParameterId, diedId: TextId) extends Extractor {
        private lazy val died: Map[PatientId, Boolean] = db
            .textSignalsAsMap(0, Long.MaxValue, parameterId)
            .mapValues(_ contains diedId)
    
        override def extract(patientId: PatientId, timeStep: Int): Option[Double] =
            died.get(patientId).map {
                case true => 1.0
                case _ => 0.0
            }
    }
    
    object ICMProperty extends Enumeration {
        val Type, System, Site, Process, Condition = Value
    }
    
    class ICMExtractor(property: ICMProperty.Value, parameterId: ParameterId) extends Extractor {
        private lazy val codes: Map[PatientId, ICM] = db
            .freeTextSignals(0, Long.MaxValue, List(parameterId))
            .map(p => (p.patientId, ICM.parse(p.value)))
            .filter(_._2.isDefined)
            .map({ case (p, icm) => (p, icm.get) })
            .toMap
        
        override def extract(patientId: PatientId, timeStep: Int): Option[Double] = {
            codes.get(patientId).map(p => property match {
                case ICMProperty.Type => p.surgical
                case ICMProperty.System => p.system
                case ICMProperty.Site => p.site
                case ICMProperty.Process => p.process
                case ICMProperty.Condition => p.condition
            })
        }
    }
    
    class PatientIdExtractor extends Extractor {
        override def extract(patientId: PatientId, timeStep: Int): Option[Double] = Some(patientId.toDouble)
    }
    
    class TimeStepExtractor extends Extractor {
        override def extract(patientId: PatientId, timeStep: Int): Option[Double] = Some(timeStep.toDouble)
    }
}