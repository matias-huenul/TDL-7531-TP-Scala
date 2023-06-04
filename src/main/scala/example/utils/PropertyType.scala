package example.utils

object PropertyType extends Enumeration{
  val DEPARTAMENTO: PropertyType.Value = Value("DEPARTAMENTO")
  val CASA: PropertyType.Value = Value("CASA")
  val PH: PropertyType.Value = Value("PH")
  def fromString(propType: String): PropertyType.Value = {
    propType.toUpperCase() match {
      case "DEPARTAMENTO" => DEPARTAMENTO
      case "CASA" => CASA
      case "PH" => PH
      case _ => DEPARTAMENTO
    }
  }
}
