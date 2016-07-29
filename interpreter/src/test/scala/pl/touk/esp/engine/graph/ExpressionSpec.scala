package pl.touk.esp.engine.graph

import java.text.ParseException
import java.time.LocalDate

import cats.data.Validated.{Invalid, Valid}
import org.scalatest.{FlatSpec, Matchers}
import pl.touk.esp.engine.Interpreter.ContextImpl
import pl.touk.esp.engine.api.{Context, MetaData}
import pl.touk.esp.engine.InterpreterConfig
import pl.touk.esp.engine.spel.SpelExpressionParser

import scala.collection.JavaConverters._
import scala.beans.BeanProperty

class ExpressionSpec extends FlatSpec with Matchers {

  val testValue = Test( "1", 2, List(Test("3", 4), Test("5", 6)).asJava)
  val ctx = ContextImpl(
    config = InterpreterConfig(
      services = Map.empty,
      listeners = Seq.empty,
      expressionFunctions = Map("today" -> classOf[LocalDate].getDeclaredMethod("now"))
    ),
    processMetaData = MetaData("process1"),
    variables = Map("obj" -> testValue)
  )

  case class Test(@BeanProperty id: String, @BeanProperty value: Long, @BeanProperty children: java.util.List[Test] = List[Test]().asJava)

  private def parse(expr: String) = {
    SpelExpressionParser.parse(expr) match {
      case Valid(e) => e
      case Invalid(err) => throw new ParseException(err.message, -1)
    }
  }

  it should "invoke simple expression" in {

    parse("#obj.value + 4").evaluate[Long](ctx) should equal(6)

  }

  it should "filter by list predicates" in {

    parse("#obj.children.?[id == '55'].empty").evaluate[Boolean](ctx) should equal(true)
    parse("#obj.children.?[id == '5'].size()").evaluate[Integer](ctx) should equal(1: Integer)

  }

  it should "perform date operations" in {
    val twoDaysAgo = LocalDate.now().minusDays(2)
    val withDays = ctx.withVariable("date", twoDaysAgo)

    parse("#date.until(T(java.time.LocalDate).now()).days").evaluate[Integer](withDays) should equal(2)
  }

  it should "register functions" in {
    val twoDaysAgo = LocalDate.now().minusDays(2)
    val withDays = ctx.withVariable("date", twoDaysAgo)

    parse("#date.until(#today()).days").evaluate[Integer](withDays) should equal(2)
  }


}