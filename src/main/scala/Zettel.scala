package zettelgeist

import java.io.{ File, FileReader }

import cats.syntax.either._
import io.circe.{ Error, yaml }
import io.circe.generic.auto._
import io.circe.yaml
import scala.util._

case class Zettel(
  title: Option[String] = None,
  tags: Option[List[String]] = None,
  mentions: Option[List[String]] = None,
  cite: Option[Citation] = None,
  dates: Option[Dates] = None,
  bibkey: Option[String] = None,
  bibtex: Option[String] = None,
  ris: Option[String] = None,
  inline: Option[String] = None,
  url: Option[String] = None,
  summary: Option[String] = None, // concise summary about the resource (optional)
  comment: Option[String] = None, // general commentary about the note (optional)
  note: Option[String] = None // actual body/content of the note
)

case class Dates(year: String, era: Option[String])

case class Citation(bibkey: String, page: Option[String])

object ZettelLoader {
  def apply(file: File): Stream[Zettel] = {
    val input = scala.io.Source.fromFile(file)
    println(s"Processing ${file.getName}")

    val text = input.mkString
    // split documents by ---; ^---\n and \n---\n ensure that embedded "---" is ignored
    val fassText = text.split(raw"(^\-\-\-\n|^\-\-\-\r\n|\n\-\-\-\n|\r\n\-\-\-\r\n)")
    if (fassText.length > 0)
      println(s"- Processing Fass with ${fassText.length} entries")
    else
      println(s"- Processing Stein")
    val fassZettels = Array.ofDim[Zettel](fassText.length)
    for (i <- 0 until fassText.length) {
      println(s"- Processing Fass @ $i")

      val attempt = Try {
        val json = yaml.parser.parse(fassText(i))
        json.leftMap(err => err: Error).flatMap(_.as[Zettel]).valueOr(throw _)
      }

      attempt match {
        case Success(s) =>
          fassZettels(i) = attempt.get
        case Failure(error) =>
          if (error.getMessage == "null")
            println("Warning: First zettel is not a YAML document (possibly ok)")
          else
            println(s"-- ${error.getMessage}")
          fassZettels(i) = new Zettel()
      }

    }
    fassZettels.toStream // to preserve the existing contract
  }
}
