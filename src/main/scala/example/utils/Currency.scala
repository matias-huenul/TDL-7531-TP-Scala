package example.utils

object Currency extends Enumeration {
  val ARS: Currency.Value = Value("ARS")
  val USD: Currency.Value = Value("USD")

  def fromString(s: String): Currency.Value = {
    s.toUpperCase() match {
      case "ARS" => ARS
      case "USD" => USD
      case "U$S" => USD
      case "PESOS" => ARS
      case "DOLARES" => USD
      case "DÓLARES" => USD
      case _ => ARS
    }
  }

}
