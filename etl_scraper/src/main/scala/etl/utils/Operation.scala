package etl.utils

object Operation extends Enumeration {
  val VENTA: Operation.Value = Value("VENTA")
  val ALQUILER: Operation.Value = Value("ALQUILER")

  def fromString(s: String): Operation.Value = {
    s.toLowerCase() match {
      case "venta" => VENTA
      case "alquiler" => ALQUILER
      case _ => ALQUILER
    }
  }
}
