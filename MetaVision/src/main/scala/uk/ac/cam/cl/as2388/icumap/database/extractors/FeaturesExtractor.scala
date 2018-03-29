package uk.ac.cam.cl.as2388.icumap.database.extractors

import java.io.{File, FileReader}

import breeze.io.CSVReader
import breeze.linalg.DenseMatrix
import org.joda.time.Instant
import uk.ac.cam.cl.as2388.icumap.database.db.MVDatabase
import uk.ac.cam.cl.as2388.icumap.database.{ParameterId, PatientId}

import scala.language.postfixOps
import scala.util.Try

object FeaturesExtractor {
    case class Parameter(parameterId: Option[ParameterId], name: String, dataType: Option[String],
                         argument: Option[String])

    class BatchedExecutor(minPatient: PatientId, maxPatient: PatientId, batchSize: Int) {
        /**
          * Given a function which expects a (minPatient, maxPatient) pair,
          * executes this function in batches of size batchSize between minPatient and maxPatient
          */
        def apply[T](f: (PatientId, PatientId) => T): Unit =
            (minPatient / batchSize until maxPatient / batchSize)
                .map(batchSize *)
                .reverse
                .sliding(2)
                .toList
                .foreach({case Vector(max, min) =>
                    f(min, max)
                })
    }
    
    def run(directory: String, parametersFileName: String, outputDirectory: String, minId: PatientId, maxId: PatientId,
            batchSize: Int, timeStepSize: Instant, db: MVDatabase): Unit = {
        val parameters: List[Parameter] = loadParametersFile(parametersFileName)
        val extractorFactory = new ExtractorFactory(db)
        val extractors: List[Extractor] = parameters.flatMap(param => extractorFactory.generate(param, timeStepSize))
       
        println("Extracting patients")
        val patients = db.admissions(minId, maxId)
        
        var nextOutput = DenseMatrix.zeros[Double](0, extractors.length)
        var idOfNextOutput = maxId + batchSize
        patients.filter(_.dischargeDate.isDefined).reverse.foreach(patient => {
            if (patient.patientId < idOfNextOutput) {
                while (patient.patientId < idOfNextOutput) {
                    idOfNextOutput -= batchSize
                }
                println("Writing " + s"$outputDirectory/fv-${idOfNextOutput + batchSize}.csv")
                
                val file = new File(s"$outputDirectory/fv-${idOfNextOutput + batchSize}.csv")
                file.getParentFile.mkdirs
                breeze.linalg.csvwrite(
                    file,
                    nextOutput
                )
                
                nextOutput = DenseMatrix.zeros[Double](0, extractors.length)
            }
            
            if (patient.patientId % 10 == 0) println(s"Extracting ${patient.patientId}")
    
            val cols = extractors.length
            val rows = extractorFactory.timeStepFor(patient.patientId, patient.dischargeDate.get, timeStepSize) + 1
    
            val outputMatrix = DenseMatrix.zeros[Double](rows, cols)
            for (timeStep <- 0 until rows; parameter <- 0 until cols) {
                outputMatrix(timeStep, parameter) = extractors(parameter).extract(patient.patientId, timeStep) match {
                    case Some(x) => x
                    case None => Double.NaN
                }
            }
    
            nextOutput = DenseMatrix.vertcat(outputMatrix, nextOutput)
        })
    }
    
    private def loadParametersFile(fileName: String): List[Parameter] =
        CSVReader
            .read(new FileReader(fileName))
            .map(x => Parameter(
                Try(x(0).toLong).toOption,
                x(1),
                if (x(2) == "") None else Some(x(2)),
                if (x.length < 4 || x(3) == "") None else Some(x(3))
            )).toList
}
