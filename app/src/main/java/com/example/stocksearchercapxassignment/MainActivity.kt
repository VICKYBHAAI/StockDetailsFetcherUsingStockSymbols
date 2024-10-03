package com.example.stocksearchercapxassignment

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException

class MainActivity : AppCompatActivity() {
    private lateinit var stockPriceTextView: TextView
    private lateinit var exchangeTextView: TextView
    private lateinit var companyTextView: TextView
    private lateinit var symbol: EditText
    private lateinit var updateButton: Button
    private var requestQueue: RequestQueue? = null
    private var previousPrice: Double = 0.0
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val API_KEY = "WYSI5ZARVAC0DFA2" // Your Alpha Vantage API key
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Match the IDs from XML file
        stockPriceTextView = findViewById(R.id.stockPriceTextView)
        exchangeTextView = findViewById(R.id.exchangeTextView)
        companyTextView = findViewById(R.id.companyTextView)
        updateButton = findViewById(R.id.updateButton)
        symbol = findViewById(R.id.symbolName)
        requestQueue = Volley.newRequestQueue(this)

        // Fetch data when the button is clicked
        updateButton.setOnClickListener {
            val symbolString = symbol.text.toString().trim()
            if (symbolString.isNotEmpty()) {
                updateButton.isEnabled = false
                fetchStockPrice(symbolString)
            } else {
                Toast.makeText(this, "Please enter a valid symbol", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchStockPrice(symbolString: String) {
        val apiUrl = "https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=$symbolString&interval=1min&apikey=$API_KEY"

        val request = JsonObjectRequest(Request.Method.GET, apiUrl, null,
            { response ->
                try {
                    Log.d("API_RESPONSE", response.toString())
                    val timeSeries = response.getJSONObject("Time Series (1min)")
                    val latestTime = timeSeries.keys().next()
                    val stockData = timeSeries.getJSONObject(latestTime)
                    val stockPrice = stockData.getString("1. open")

                    // After fetching stock price, fetch company details
                    fetchCompanyDetails(symbolString, stockPrice.toDouble())
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error parsing stock price response", Toast.LENGTH_SHORT).show()
                    updateButton.isEnabled = true
                }
            },
            { error ->
                if (error.networkResponse != null) {
                    val responseBody = String(error.networkResponse.data)
                    Log.e("API_ERROR", "Error Response: $responseBody")
                } else {
                    Log.e("API_ERROR", "Network error: ${error.message}")
                }
                updateButton.isEnabled = true
            })

        requestQueue!!.add(request)
    }

    private fun fetchCompanyDetails(symbolString: String, stockPrice: Double) {
        val companyApiUrl = "https://www.alphavantage.co/query?function=OVERVIEW&symbol=$symbolString&apikey=$API_KEY"

        val request = JsonObjectRequest(Request.Method.GET, companyApiUrl, null,
            { response ->
                try {
                    Log.d("COMPANY_RESPONSE", response.toString())
                    val companyName = response.getString("Name")
                    val exchange = response.getString("Exchange")

                    // Update UI with stock price, exchange, and company name
                    updateStockDetails(stockPrice, exchange, companyName)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error parsing company details response", Toast.LENGTH_SHORT).show()
                } finally {
                    updateButton.isEnabled = true
                }
            },
            { error ->
                if (error.networkResponse != null) {
                    val responseBody = String(error.networkResponse.data)
                    Log.e("API_ERROR", "Error Response: $responseBody")
                } else {
                    Log.e("API_ERROR", "Network error: ${error.message}")
                }
                updateButton.isEnabled = true
            })

        requestQueue!!.add(request)
    }

    private fun updateStockDetails(currentPrice: Double, exchange: String, company: String) {
        val priceChange = currentPrice - previousPrice
        previousPrice = currentPrice

        handler.post {
            startBlinkAnimation(stockPriceTextView)
            val formattedPrice = String.format("%.2f", currentPrice)
            stockPriceTextView.text = formattedPrice
            stockPriceTextView.setTextColor(if (priceChange >= 0) Color.GREEN else Color.RED)
            exchangeTextView.text = "Exchange: $exchange"
            companyTextView.text = "Company Name: $company"
        }
    }

    private fun startBlinkAnimation(textView: TextView) {
        val blink = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f, 1f)
        blink.duration = 200
        blink.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        requestQueue?.stop()
    }
}
