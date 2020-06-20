package io.mwielocha.githubsync.db

import com.github.tminglei.slickpg.PgCirceJsonSupport
import io.circe.{ Json, Printer }
import io.circe.parser.parse
import slick.jdbc.JdbcType
import io.circe.syntax._
import com.github.tminglei.slickpg._
import slick.basic.Capability
import slick.driver.JdbcProfile
import slick.jdbc.JdbcCapabilities

trait PostgresProfile extends slick.jdbc.PostgresProfile with PgCirceJsonSupport {

  override val pgjson: String = "jsonb"

  override val api: JsonbApi.type = JsonbApi

  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + JdbcCapabilities.insertOrUpdate

  private val jsonPrinter = Printer.spaces2.copy(dropNullValues = true)

  ///
  object JsonbApi extends API with JsonImplicits {
    override implicit val circeJsonTypeMapper: JdbcType[Json] =
      new GenericJdbcType[Json](
        pgjson,
        v => parse(v).getOrElse(Json.Null),
        v => v.asJson.printWith(jsonPrinter),
        hasLiteralForm = false
      )
  }
}

object PostgresProfile extends PostgresProfile
