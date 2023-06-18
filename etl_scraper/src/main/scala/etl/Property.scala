package etl

import etl.utils.{Currency, Operation, Page, PropertyType}

class Property(
                  var url: String = "",
                  var propType: PropertyType.Value = PropertyType.DEPARTAMENTO,
                  var operation: Operation.Value = Operation.ALQUILER,
                  var price: Int = 0,
                  var currency: Currency.Value = Currency.ARS,
                  var expenses: Int = 0,
                  var totalSurf: Int = 0,
                  var coveredSurf: Int = 0,
                  var rooms: Int = 0,
                  var bedrooms: Int = 0,
                  var bathrooms: Int = 0,
                  var address: String = "",
                  var barrio: String = "",
                  var garage: Int = 0,
                  var page: Page.Value = Page.ZONAPROP) {

  def setOperation(s: String): Unit = {
    operation = Operation.fromString(s)
  }

  def setCurrency(s: String): Unit = {
    currency = Currency.fromString(s)
  }

  def setPropertyType(s: String): Unit = {
    propType = PropertyType.fromString(s)
  }

  override def toString: String = {
    "Propiedad: " + url + "\n" +
    "    Tipo " + propType +
    "    Operacion " + operation +
    "    Precio " + currency + price +
    "    Expensas " + expenses + "\n" +
    " Caracteristicas: \n" +
    "    Superficie Total " + totalSurf +
    "    Superficie Cubierta " + coveredSurf +
    "    Ambientes " + rooms +
    "    Dormitorios " + bedrooms +
    "    Baños " + bathrooms +
    "    Cochera " + garage + "\n" +
    "  Dirceccion: \n" +
    "    Direccion " + address +
    "    Barrio " + barrio

  }

}

