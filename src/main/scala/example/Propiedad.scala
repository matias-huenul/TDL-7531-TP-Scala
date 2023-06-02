package example

import example.utils.Operation
import example.utils.Currency

class Propiedad (
  var id: String = "",
  var url: String = "",
  var tipo: String = "",
  var operacion: Operation.Value = Operation.ALQUILER,
  var precio: Int = 0,
  var moneda: Currency.Value = Currency.ARS,
  var expensas: Int = 0,
  var superficeTotal: Int = 0,
  var superficieCubierta: Int = 0,
  var ambientes: Int = 0,
  var dormitorios: Int = 0,
  var banios: Int = 0,
  var direccion: String = "",
  var coordenadas: String = "",
  var barrio: String = "",
  var cochera: Int = 0) {

override def toString: String = {
    "Propiedad: " + url + "\n" +
    "    Tipo " + tipo +
    "    Operacion " + operacion +
    "    Precio " + moneda + precio +
    "    Expensas " + expensas + "\n" +
    " Caracteristicas: \n" +
    "    Superficie Total " + superficeTotal +
    "    Superficie Cubierta " + superficieCubierta +
    "    Ambientes " + ambientes +
    "    Dormitorios " + dormitorios +
    "    Baños " + banios +
    "    Cochera " + cochera + "\n" +
    "  Dirceccion: \n" +
    "    Direccion " + direccion +
    "    Barrio " + barrio +
    "    Coordenadas " + coordenadas

  }

}

