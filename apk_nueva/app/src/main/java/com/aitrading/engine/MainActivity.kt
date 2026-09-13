package com.aitrading.engine

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val bridgeUrl =
        "https://probable-space-rotary-phone-jrq4gxjwvx4g3pwv4-8080.app.github.dev"

    private var motorActivo = false
    private var emergencia = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnVerificar).setOnClickListener { verificar() }
        findViewById<Button>(R.id.btnMotor).setOnClickListener { cambiarMotor() }
        findViewById<Button>(R.id.btnEmergencia).setOnClickListener { activarEmergencia() }
        findViewById<Button>(R.id.tabInicio).setOnClickListener { mostrar("inicio") }
        findViewById<Button>(R.id.tabOps).setOnClickListener { mostrar("ops") }
        findViewById<Button>(R.id.tabMotor).setOnClickListener { mostrar("motor") }
        findViewById<Button>(R.id.tabAjustes).setOnClickListener { mostrar("ajustes") }
        findViewById<Button>(R.id.tabCal).setOnClickListener { mostrar("cal") }
        findViewById<Button>(R.id.btnCompra).setOnClickListener { enviarOrden("BUY") }
        findViewById<Button>(R.id.btnVenta).setOnClickListener { enviarOrden("SELL") }
        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val cuenta = findViewById<EditText>(R.id.edtCuenta).text.toString().trim()
            val pass = findViewById<EditText>(R.id.edtPass).text.toString().trim()
            val server = findViewById<EditText>(R.id.edtServer).text.toString().trim()
            if (cuenta.isEmpty() || pass.isEmpty() || server.isEmpty()) {
                Toast.makeText(this, "Debes llenar cuenta, contrasena y servidor", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val prefs = getSharedPreferences("login", MODE_PRIVATE)
            val savedC = prefs.getString("cuenta", "") ?: ""
            val savedP = prefs.getString("pass", "") ?: ""
            val savedS = prefs.getString("server", "") ?: ""
            if (savedC.isEmpty()) {
                prefs.edit()
                    .putString("cuenta", cuenta)
                    .putString("pass", pass)
                    .putString("server", server)
                    .apply()
            } else if (cuenta != savedC || pass != savedP || server != savedS) {
                Toast.makeText(this, "Cuenta, contrasena o servidor incorrectos", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            findViewById<android.view.View>(R.id.panelLogin).visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.panelApp).visibility = android.view.View.VISIBLE
            findViewById<android.view.View>(R.id.barraTabs).visibility = android.view.View.VISIBLE
            mostrar("inicio")
            verificar()
        }
    }

    private fun pintarTabs(tab: String) {
        val morado = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#7B2CBF"))
        val gris = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#333333"))
        findViewById<Button>(R.id.tabInicio).backgroundTintList = if (tab == "inicio") morado else gris
        findViewById<Button>(R.id.tabOps).backgroundTintList = if (tab == "ops") morado else gris
        findViewById<Button>(R.id.tabMotor).backgroundTintList = if (tab == "motor") morado else gris
        findViewById<Button>(R.id.tabAjustes).backgroundTintList = if (tab == "ajustes") morado else gris
        findViewById<Button>(R.id.tabCal).backgroundTintList = if (tab == "cal") morado else gris
    }
    private fun mostrar(tab: String) {
        pintarTabs(tab)
        val inicio = findViewById<android.view.View>(R.id.panelInicio)
        val ops = findViewById<android.view.View>(R.id.panelOps)
        val motor = findViewById<android.view.View>(R.id.panelMotor)
        val ajustes = findViewById<android.view.View>(R.id.panelAjustes)
        val cal = findViewById<android.view.View>(R.id.panelCal)
        inicio.visibility = if (tab == "inicio") android.view.View.VISIBLE else android.view.View.GONE
        ops.visibility = if (tab == "ops") android.view.View.VISIBLE else android.view.View.GONE
        motor.visibility = if (tab == "motor") android.view.View.VISIBLE else android.view.View.GONE
        ajustes.visibility = if (tab == "ajustes") android.view.View.VISIBLE else android.view.View.GONE
        cal.visibility = if (tab == "cal") android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun cambiarMotor() {
        if (emergencia) {
            findViewById<TextView>(R.id.txtDetalle).text =
                "Emergencia activa. No se puede activar el motor."
            return
        }
        motorActivo = !motorActivo
        actualizarMotor()
    }

    private fun activarEmergencia() {
        emergencia = !emergencia
        if (emergencia) {
            motorActivo = false
            actualizarMotor()
            findViewById<TextView>(R.id.txtDetalle).text =
                "Emergencia: bloquea nuevas operaciones. No cierra posiciones."
        } else {
            motorActivo = false
            actualizarMotor()
            findViewById<TextView>(R.id.txtDetalle).text =
                "Emergencia desactivada. Motor en PAUSADO."
        }
    }

    private fun actualizarMotor() {
        val txt = findViewById<TextView>(R.id.txtMotor)
        txt.text = when {
            emergencia -> "MOTOR IA: EMERGENCIA"
            motorActivo -> "MOTOR IA: ACTIVO"
            else -> "MOTOR IA: PAUSADO"
        }
    }

    private fun verificar() {
        val txtEstado = findViewById<TextView>(R.id.txtEstado)
        val txtBalance = findViewById<TextView>(R.id.txtBalance)
        val txtEquity = findViewById<TextView>(R.id.txtEquity)
        val txtMargen = findViewById<TextView>(R.id.txtMargen)
        val txtDetalle = findViewById<TextView>(R.id.txtDetalle)
        val txtDetalleInicio = findViewById<TextView>(R.id.txtDetalleInicio)

        txtEstado.text = "VERIFICANDO..."
        txtDetalle.text = "Consultando Bridge..."
        txtDetalleInicio.text = "Consultando Bridge..."

        thread {
            try {
                val health = getJson("$bridgeUrl/health")
                val status = getJson("$bridgeUrl/mt5/status")
                val ok = health.optBoolean("ok", false)
                val bridge = health.optString("bridge", "offline")
                val mt5 = status.optString("mt5", "pending")

                runOnUiThread {
                    if (ok && bridge == "online") {
                        txtEstado.text = "VERIFICADO"
                        txtEstado.setTextColor(getColor(R.color.verde))
                    } else {
                        txtEstado.text = "SIN VERIFICAR"
                    }
                    txtBalance.text = "Balance\n${num(status, "balance", "BALANCE", "Balance")}"
                    txtEquity.text = "Equity\n${num(status, "equity", "EQUITY", "Equity")}"
                    txtMargen.text = "Margen\n${num(status, "free_margin", "FREE_MARGIN", "freeMargin", "margen", "MARGIN")}"
                    val fav = findViewById<TextView>(R.id.txtFavoritos)
                    val sym = status.optString("symbol", "BTCUSD")
                    fav.text = "$sym  BID ${num(status, "bid", "BID")}  ASK ${num(status, "ask", "ASK")}"
                    txtDetalleInicio.text = "Bridge: $bridge | MT5: $mt5"
                    pintarOperaciones(status)
                    try {
                        val pr = status.opt("profit")
                        val npos = when {
                            status.optJSONArray("positions") != null -> status.optJSONArray("positions").length()
                            status.optJSONArray("open_positions") != null -> status.optJSONArray("open_positions").length()
                            else -> if (pr != null && pr.toString() != "null" && pr.toString() != "0") 1 else 0
                        }
                        findViewById<TextView>(R.id.txtDetalleInicio).text =
                            "POSICIONES ABIERTAS: $npos | Ganancia flotante: $pr"
                    } catch (_: Exception) {}
                    analizarVelas(status)
                    val aj = findViewById<TextView>(R.id.txtAjustes)
                    aj.text = "Bridge:\n" + bridgeUrl +
                        "\n\nModo: " + status.optString("mode", "DEMO") +
                        "\nSimbolo: " + status.optString("requested_symbol", "-") +
                        "\nTimeframe: " + status.optString("requested_timeframe", "-") +
                        "\nMT5: " + status.optString("mt5", "-") +
                        "\n\nEstos valores vienen del Bridge. Aun no hay comando para cambiarlos desde la APK."
                    val info = findViewById<TextView>(R.id.txtMotorInfo)
                    info.text = "Estado: " + (if (emergencia) "EMERGENCIA" else if (motorActivo) "ACTIVO" else "PAUSADO") +
                        "\nModo: " + status.optString("mode", "DEMO") +
                        "\nSimbolo: " + status.optString("requested_symbol", "-") +
                        "\nTimeframe: " + status.optString("requested_timeframe", "-") +
                        "\nBid: " + status.opt("bid") + "  Ask: " + status.opt("ask") +
                        "\n\nLa IA aun no envia senal. Esta pantalla solo muestra estado real del Bridge."
                }
            } catch (e: Exception) {
                runOnUiThread {
                    txtEstado.text = "DESCONECTADO"
                    txtDetalle.text = "Error: ${e.message}"
                }
            }
        }
    }

    private fun pintarOperaciones(status: JSONObject) {
        val txt = findViewById<TextView>(R.id.txtDetalle)
        val profit = status.opt("profit")
        val pending = status.opt("pending_order")
        val keys = listOf("positions", "open_positions", "orders", "abiertas")
        var lista = ""
        for (k in keys) {
            val arr = status.optJSONArray(k)
            if (arr != null && arr.length() > 0) {
                val sb = StringBuilder()
                for (i in 0 until arr.length()) {
                    sb.append(arr.get(i).toString()).append("\n")
                }
                lista = sb.toString()
                break
            }
        }
        val result = status.opt("last_order_result")
        val margen = status.opt("margin")
        val equity = status.opt("equity")
        findViewById<TextView>(R.id.txtGanancia).text = "$profit USD"
        var npos = 0
        try {
            if (margen != null && margen.toString() != "null" && margen.toString().toDouble() > 0) npos = 1
        } catch (_: Exception) {}
        if (result != null && result.toString().contains("ticket")) npos = maxOf(npos, 1)
        findViewById<TextView>(R.id.txtPosiciones).text = npos.toString()
        var riesgo = "0.00%"
        try {
            val m = margen.toString().toDouble()
            val e = equity.toString().toDouble()
            if (e > 0) riesgo = String.format("%.4f%%", (m / e) * 100.0)
        } catch (_: Exception) {}
        findViewById<TextView>(R.id.txtRiesgo).text = riesgo
        val ticket = if (result is org.json.JSONObject) result.opt("ticket") else null
        txt.text = "BTCUSD BUY 0.01\nTicket: $ticket\nP/L: $profit\nMargen: $margen"
    }


    private fun num(obj: JSONObject, vararg keys: String): String {
        for (k in keys) {
            if (obj.has(k) && !obj.isNull(k)) {
                val v = obj.opt(k)
                if (v != null && v.toString() != "null" && v.toString().isNotBlank()) {
                    return v.toString()
                }
            }
        }
        return "0.00"
    }

    private fun enviarOrden(lado: String) {
        val det = findViewById<TextView>(R.id.txtDetalle)
        det.text = "Enviando $lado DEMO..."
        thread {
            try {
                val id = "apk-" + System.currentTimeMillis()
                val body = """{"id":"$id","direction":"$lado","symbol":"BTCUSD","volume":0.01}"""
                val url = java.net.URL("$bridgeUrl/mt5/order/pending")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("X-Bridge-Token", "DEMO-ZEUS-2026")
                conn.outputStream.write(body.toByteArray())
                val code = conn.responseCode
                val txt = (if (code < 400) conn.inputStream else conn.errorStream).bufferedReader().readText()
                runOnUiThread { det.text = "$lado HTTP=$code\n$txt"; verificar() }
            } catch (e: Exception) {
                runOnUiThread { det.text = "Error orden: ${e.message}" }
            }
        }
    }
    private fun ema(closes: List<Double>, n: Int): Double {
        if (closes.size < n) return 0.0
        val k = 2.0 / (n + 1)
        var e = closes.take(n).average()
        for (j in n until closes.size) e = closes[j] * k + e * (1 - k)
        return e
    }
    private fun setTxt(id: Int, value: String) {
        try { findViewById<TextView>(id).text = value } catch (_: Exception) {}
    }
    private fun analizarVelas(status: JSONObject): String {
        val arr = status.optJSONArray("candles") ?: return "Sin velas"
        if (arr.length() < 21) return "Velas insuficientes: ${arr.length()}"
        val closes = mutableListOf<Double>()
        for (n in 0 until arr.length()) {
            val o = arr.optJSONObject(n) ?: continue
            closes.add(o.optDouble("close", 0.0))
        }
        val e9 = ema(closes, 9)
        val e21 = ema(closes, 21)
        val e50 = ema(closes, 50)
        val last = closes.last()
        val tendencia = when {
            last > e21 && e9 > e21 -> "ALCISTA"
            last < e21 && e9 < e21 -> "BAJISTA"
            else -> "LATERAL"
        }
        setTxt(R.id.txtEma, "EMA9 ${"%.2f".format(e9)}  EMA21 ${"%.2f".format(e21)}  EMA50 ${"%.2f".format(e50)}")
        setTxt(R.id.txtEscenario, "ESCENARIO: $tendencia  velas ${closes.size}  close $last")
        setTxt(R.id.txtProb, "TENDENCIA $tendencia · 9 patrones sin confluencia multi-TF")
        val pat = patronesSimples(closes)
        setTxt(R.id.txtPatrones, pat + "  Sin 2a TF = no opera.")
        setTxt(R.id.txtMotorInfo, "Velas ${closes.size}  Precio $last  $tendencia. No opera solo.")
        return tendencia
    }
    private fun patronesSimples(closes: List<Double>): String {
        if (closes.size < 40) return "Patrones: velas cortas"
        val w = closes.takeLast(80)
        val max1 = w.maxOrNull() ?: return "Patrones: --"
        val min1 = w.minOrNull() ?: return "Patrones: --"
        val iMax = w.indexOf(max1)
        val iMin = w.indexOf(min1)
        val nearTop = w.filter { kotlin.math.abs(it - max1) / max1 < 0.0025 }.size
        val nearBot = w.filter { kotlin.math.abs(it - min1) / min1 < 0.0025 }.size
        return when {
            nearTop >= 2 && iMax < w.size - 5 -> "Posible doble techo"
            nearBot >= 2 && iMin < w.size - 5 -> "Posible doble suelo"
            else -> "Sin figura clara (bandera/cuna/triangulo no confirmados)"
        }
    }
    private fun getJson(url: String): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")
        conn.inputStream.bufferedReader().use {
            return JSONObject(it.readText())
        }
    }
}
