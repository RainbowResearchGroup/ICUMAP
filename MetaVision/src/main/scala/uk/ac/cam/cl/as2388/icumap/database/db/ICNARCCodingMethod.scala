package uk.ac.cam.cl.as2388.icumap.database.db

import java.nio.file.{Files, Paths}

import net.ruippeixotog.scalascraper.browser.JsoupBrowser

import scala.io.Source
import scala.util.matching.Regex

/**
  * Represents an ICNARC Coding Method condition code.
  * https://www.icnarc.org/Our-Audit/Audits/Cmp/Resources/Icm-Icnarc-Coding-Method
  */
case class ICM(surgical: Int, system: Int, site: Int, process: Int, condition: Int) {
    override def toString: String = s"$surgical.$system.$site.$process.$condition"
    
    /**  */
    def toConditionString: Option[String] = ICM.conditionForCode(this)
}

object ICM {
    // Map from the system.site.process.condition portion of a condition code to the condition string.
    private lazy val codes: Map[String, String] = {
        if (!Files.exists(Paths.get("icm-database.txt"))) buildDatabase()
        loadBuiltDatabase()
    }
    
    /**
      * Converts the given string to an ICNARC Code.
      * Note that this accepts all codes which are syntactically valid, not just those which are defined by ICNARC.
      */
    def parse(icm: String): Option[ICM] = {
        val scheme: Regex = raw"([1|2]).([0-9]+).([0-9]+).([0-9]+).([0-9]+)".r
        
        icm.trim match {
            case scheme(a, b, c, d, e) => Some(ICM(a.toInt, b.toInt, c.toInt, d.toInt, e.toInt))
            case _ => None
        }
    }
    
    /** Returns the condition string for the presented condition code. */
    def conditionForCode(code: ICM): Option[String] = codes.get(code.toString.drop(2)) // (Drops the surgical code first)
    
    /** Constructs a file icm-database.txt by scraping ICNARC's ICM website.
      * icm-database.txt contains a map from condition codes to condition strings.
      */
    def buildDatabase(): Unit = {
        println("Building ICM database...")
        
        import java.io._
        val pw = new PrintWriter(new File("icm-database.txt"))
        
        for (
            system <- 1 to 15;
            site <- 1 to 50;
            process <- 1 to 90
        ) {
            val code = s"$system.$site.$process"
            val source = s"https://www.icnarc.org/Modules/ICM/1.$code"
            
            val fetcher = JsoupBrowser()
            val rawPage = fetcher.get(source)
            val page: Array[String] = rawPage.toString.split("\n")
            
            val scheme: Regex = raw""".*<li><a.*icmcode="(([0-9]|.)*)" class=""><span class="condition-number">.*</span> - <span class="condition-desc">(.*)</span></a></li>.*""".r
            page.foreach {
                case scheme(a, _, c, _*) => println(s"$a: $c"); pw.write(s"$a: $c\n")
                case _ => ()
            }
        }
        
        pw.close()
    }
    
    /** Generates a map from system.site.process.condition to condition string by reading the cached file
      * produced by this.buildDatabase()
      */
    def loadBuiltDatabase(): Map[String, String] = {
        val lines = Source.fromFile("icm-database.txt").getLines().toList
        val scheme: Regex = raw"""(([0-9]|.)*): (.*)""".r
        
        lines.flatMap {
            case scheme(a, _, c, _*) => Some(a.drop(2), c)
            case _ => None
        }.toMap
    }
}
