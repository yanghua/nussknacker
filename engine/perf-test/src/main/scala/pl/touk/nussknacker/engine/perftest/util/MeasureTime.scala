package pl.touk.nussknacker.engine.perftest.util

object MeasureTime {

  def in[T](f: => T): (T, Long) = {
    val before =  System.currentTimeMillis()
    (f, System.currentTimeMillis() - before)
  }

}
