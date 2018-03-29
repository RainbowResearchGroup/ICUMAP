package uk.ac.cam.cl.as2388.icumap.database.extractors

import uk.ac.cam.cl.as2388.icumap.database.extractors.configurations.ExtractorConfiguration

import scala.language.postfixOps

object ExtractEverything {
    val config: ExtractorConfiguration = ???
    
    def main(args: Array[String]): Unit = {
        this.testDBConnection()
        this.extract()
    }
    
    def testDBConnection(): Unit = {
        println("Total number of admissions: " + config.db.admissions().length)
    }
    
    def generateParametersTemplate(): Unit = {
    
    }
    
    def extract(): Unit = {
        if (config.extractAttributes) {
            PatientAttributes.run(
                attributesOutputPath = config.attributesOutputPath,
                minId = config.minId,
                maxId = config.maxId,
                freeTextAttributes = config.freeTextAttributes,
                textAttributes = config.textAttributes,
                attributesToStrip = config.attributesToExclude,
                stripAttributesFromPatient = config.excludeAttributesFromPatient,
                customExtractors = config.customAttributes,
                db = config.db
            )
        }
    
        if (config.extractFeatures) {
            FeaturesExtractor.run(
                directory = config.dataDirectory,
                parametersFileName = s"${config.dataDirectory}/parameters.csv",
                outputDirectory = s"${config.dataDirectory}/shards",
                minId = config.minId,
                maxId = config.maxId,
                batchSize = config.batchSize,
                timeStepSize = config.timeStepSize,
                db = config.db
            )
        }
    }
}
