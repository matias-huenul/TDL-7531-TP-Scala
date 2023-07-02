package scraper.etl.utils

object PropertyType extends Enumeration{
  val DEPARTMENT: PropertyType.Value = Value("DEPARTAMENTO")
  val HOUSE: PropertyType.Value = Value("CASA")
  val PH: PropertyType.Value = Value("PH")
  def fromString(propType: String): PropertyType.Value = {
    propType.toUpperCase() match {
      case "DEPARTAMENTO" => DEPARTMENT
      case "CASA" => HOUSE
      case "PH" => PH
      case _ => DEPARTMENT
    }
  }
}
