package com.aitrading.engine

import android.os.Bundle
import android.widget.Button
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
        findViewById<Button>(R.id.btnLogin).setOnClickListener {
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
                    txtBalance.text = "Balance\n${status.opt("balance")}"
                    txtEquity.text = "Equity\n${status.opt("equity")}"
                    txtMargen.text = "Margen\n${status.opt("free_margin")}"
                    txtDetalleInicio.text = "Bridge: $bridge | MT5: $mt5"
                    pintarOperaciones(status)
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
        if (lista.isEmpty()) {
            txt.text = "Bridge: ${status.optString("mt5")} | Profit: $profit\nPendiente: $pending\nNo hay posiciones abiertas."
        } else {
            txt.text = "Profit: $profit\nPendiente: $pending\n$lista"
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
