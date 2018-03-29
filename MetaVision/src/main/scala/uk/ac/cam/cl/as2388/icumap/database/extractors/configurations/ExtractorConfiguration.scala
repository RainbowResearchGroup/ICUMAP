package uk.ac.cam.cl.as2388.icumap.database.extractors.configurations

import org.joda.time.Instant
import uk.ac.cam.cl.as2388.icumap.database.db.MVDatabase
import uk.ac.cam.cl.as2388.icumap.database.extractors.PatientAttributes
import uk.ac.cam.cl.as2388.icumap.database.util.NaturalLanguageTime.doubleToTime
import uk.ac.cam.cl.as2388.icumap.database.{ParameterId, PatientId}

import scala.language.postfixOps

/**
  *
  * @param db                           Database to extract data from.
  * @param extractAttributes            Whether to extract attributes on this run.
  * @param extractFeatures              Whether to extract feature vectors on this run.
  * @param dataDirectory                Directory into which extracted data should be saved.
  * @param minId                        Lowest PatientID to extract. Defaults to the lowest id in the database.
  * @param maxId                        Highest PatientID to extract. Defaults to highest id in the database.
  * @param batchSize                    Interval by which to group extracted patients e.g. batch size of 100 will put up
  *                                     to 100 patients in each CSV. Defaults to 100.
  * @param timeStepSize                 Length of time each time step should be. Defaults to 6 hours.
  *                                     To compensate for sensor noise and the high volume of data, all values are
  *                                     medianed within windows of this size.
  * @param freeTextAttributes           List of Parameter IDs for FreeTextSignals (free text inputs) to be extracted
  *                                     as attributes.
  * @param textAttributes               List of Parameter IDs for TextSignals (multiple choice inputs) to be extracted
  *                                     as attributes.
  * @param excludeAttributesFromPatient A function which, given a Patient ID, determines whether that patient should
  *                                     have some attributes excluded. Default is to return false for all Patient IDs.
  * @param attributesToExclude          Attributes to exclude from the patients returned by excludeAttributesFromPatient
  *                                     as true.
  * @param attributesOutputPath         File to write extracted attributes to.
  * @param customAttributes             Additional functions for extracting attributes which cannot be produced using
  *                                     the methods above. Each custom extractor should have the signature
  *                                     (minId, maxId) => List[PatientAttributes]. Some common cases are provided in
  *                                     @see [[PatientAttributes]]
  */
class ExtractorConfiguration(
    val db: MVDatabase,
    val extractAttributes: Boolean = true,
    val extractFeatures: Boolean = true,
    val dataDirectory: String,
    val minId: Int = 0,
    val maxId: Int = Int.MaxValue,
    val batchSize: Int = 100,
    val timeStepSize: Instant = new Instant().withMillis(6 hours),
    val freeTextAttributes: List[ParameterId] = List(),
    val textAttributes: List[ParameterId] = List(),
    val excludeAttributesFromPatient: (PatientId => Boolean) = _ => false,
    val attributesToExclude: Set[String] = Set(),
    val attributesOutputPath: String,
    val customAttributes: List[(PatientId, PatientId) => List[PatientAttributes]] = List()
)
