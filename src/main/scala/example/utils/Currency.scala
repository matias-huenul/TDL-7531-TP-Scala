package example.utils

object Currency extends Enumeration {
  val ARS: Currency.Value = Value("ARS")
  val USD: Currency.Value = Value("USD")

  def fromString(s: String): Currency.Value = {
    s match {
      case "ARS" => ARS
      case "USD" => USD
      case "U$S" => USD
      case "pesos" => ARS
      case "dolares" => USD
      case "dólares" => USD
      case _ => ARS
    }
  }

}
