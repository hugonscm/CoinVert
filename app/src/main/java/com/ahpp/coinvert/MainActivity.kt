package com.ahpp.coinvert

import android.R
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Xml
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ahpp.coinvert.databinding.MainActivityBinding
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.iterator
import kotlin.collections.set
import kotlin.collections.toSortedMap


class MainActivity : ComponentActivity() {

    private lateinit var binding: MainActivityBinding

    private val currencyRates = HashMap<String, String>() // Para armazenar os pares currency-rate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContent {
            ChangeSystemBarsTheme(!isSystemInDarkTheme())
            binding = MainActivityBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setXml()

            binding.editTextValorParaConverter.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable?) { }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    calcularCotacao()
                }
            })

        }
    }

    private fun setXml() {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            val xmlInputStream = downloadXmlFromUrl()
            if (xmlInputStream != null) {
                try {
                    val xmlPullParser = Xml.newPullParser()
                    xmlPullParser.setInput(xmlInputStream, null)

                    var currentTime: String? = null

                    var eventType = xmlPullParser.eventType
                    var currentCurrency = ""
                    var currentRate = ""

                    currencyRates["USD"] = "1.0"

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                when (xmlPullParser.name) {
                                    "Cube" -> {
                                        if (xmlPullParser.getAttributeValue(null, "currency") != null) {
                                            currentCurrency = xmlPullParser.getAttributeValue(null, "currency")
                                        }
                                        if (xmlPullParser.getAttributeValue(null, "rate") != null) {
                                            currentRate = xmlPullParser.getAttributeValue(null, "rate")
                                        }
                                        if (xmlPullParser.getAttributeValue(null, "time") != null) {
                                            currentTime = xmlPullParser.getAttributeValue(null, "time")
                                        }
                                    }
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (xmlPullParser.name == "Cube" && currentCurrency.isNotEmpty() && currentRate.isNotEmpty()) {
                                    currencyRates[currentCurrency] = currentRate
                                    currentCurrency = ""
                                    currentRate = ""
                                }
                            }
                        }
                        eventType = xmlPullParser.next()
                    }


                    val sortedCurrencyRates = currencyRates.toSortedMap()

                    runOnUiThread {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale("US"))
                        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale("BR"))
                        val date = currentTime?.let { inputFormat.parse(it) }
                        val formattedDate = date?.let { outputFormat.format(it) }

                        //coloca a hora depois


                        val att = "Dados obtidos do Banco Central Europeu\n Última atualização: $formattedDate"
                        binding.textViewData.text = att

                        val dataForSpinner = ArrayList<String>() // Lista para conter apenas as moedas
                        for (pair in sortedCurrencyRates)
                            dataForSpinner.add(pair.key)

                        // Crie um ArrayAdapter e defina os dados no Spinner
                        val adapter = ArrayAdapter(this, R.layout.simple_spinner_item, dataForSpinner)
                        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
                        binding.spinnerFrom.adapter = adapter
                        binding.spinnerTo.adapter = adapter

                        binding.spinnerFrom.setSelection(dataForSpinner.indexOf("USD"))
                        binding.spinnerTo.setSelection(dataForSpinner.indexOf("BRL"))
                    }
                } catch (e: XmlPullParserException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    xmlInputStream.close()
                }
            } else {
                Toast.makeText(this, "Erro de rede", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadXmlFromUrl(): InputStream? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml")
            connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                return connection.inputStream
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        return null
    }

    private fun calcularCotacao(){

        if (currencyRates.isEmpty())
            return

        if (binding.editTextValorParaConverter.text.isEmpty() || "N/A".equals(binding.editTextValorParaConverter.text)){
            binding.TextViewValorConvertido.text = "0"
            return
        }

        try {
            val de: String = binding.spinnerFrom.selectedItem.toString()
            val para: String = binding.spinnerTo.selectedItem.toString()

            val valor: String = binding.editTextValorParaConverter.text.toString()
            val cotacaoDe: String? = currencyRates[de]
            val cotacaoPara: String? = currencyRates[para]

            val result: Double = (valor.toDouble() / cotacaoDe!!.toDouble()) * (cotacaoPara!!.toDouble())
            val resultRound = String.format("%.2f", result)

            binding.TextViewValorConvertido.text = resultRound
        } catch (ignored: Exception){
            binding.TextViewValorConvertido.text = "0"
        }
    }

    @Composable
    private fun ChangeSystemBarsTheme(lightTheme: Boolean) {
        val transparent = Color.Transparent.toArgb()
        val black = Color.Black.toArgb()
        val white = Color.White.toArgb()

        if (lightTheme) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(transparent, transparent),
                navigationBarStyle = SystemBarStyle.light(white, white))
        } else {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(black),
                navigationBarStyle = SystemBarStyle.dark(black))
        }
    }
}