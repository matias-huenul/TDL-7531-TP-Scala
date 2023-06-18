package etl.utils

object Currency extends Enumeration {
  val ARS: Currency.Value = Value("ARS")
  val USD: Currency.Value = Value("USD")

  def fromString(s: String): Currency.Value = {
    s.toUpperCase() match {
      case "USD" => USD
      case "U$S" => USD
      case "DOLARES" => USD
      case "DÓLARES" => USD
      case _ => ARS
    }
  }

}
