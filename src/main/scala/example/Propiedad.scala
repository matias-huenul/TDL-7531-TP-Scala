package example

class Propiedad (
  var id: Int = 0,
  var url: String = "",
  var precio: Int = 0,
  var moneda: Int = 1,
  var expensas: Int = 0,
  var superficeTotal: Int = 0,
  var superficieCubierta: Int = 0,
  var ambientes: Int = 0,
  var dormitorios: Int = 0,
  var banios: Int = 0,
  var direccion: String = "",
  var coordenadas: String = "",
  var barrio: String = "",
  var pagina: String = "",
  var cochera: Int = 0) {

override def toString(): String = {
  val monedaStr = if (this.moneda == 1) "ARS" else "USD"

    "Propiedad: " + url +
    " Precio " + monedaStr + precio +
    " Expensas " + expensas +
    " Superficie Total " + superficeTotal +
    " Superficie Cubierta " + superficieCubierta +
    " Ambientes " + ambientes +
    " Dormitorios " + dormitorios +
    " Baños " + banios

  }

}

