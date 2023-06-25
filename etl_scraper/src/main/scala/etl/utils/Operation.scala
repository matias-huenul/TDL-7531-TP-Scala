package etl.utils

object Operation extends Enumeration {
  val SALE, RENT = Value

  def fromString(s: String): Operation.Value = {
    s.toLowerCase() match {
      case "venta" => SALE
      case "alquiler" => RENT
      case "alquiler temporal" => RENT
      case "renta" => RENT
      case "sale" => SALE
      case _ => RENT
    }
  }

  def toSpanishString(o: Operation.Value): String = {
    o match {
      case SALE => "venta"
      case RENT => "alquiler"
      case _ => "alquiler"
    }
  }
}
