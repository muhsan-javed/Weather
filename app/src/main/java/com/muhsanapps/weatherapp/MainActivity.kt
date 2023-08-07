package com.muhsanapps.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.muhsanapps.weatherapp.Models.ModelClass
import com.muhsanapps.weatherapp.Utilities.ApiUtilities
import com.muhsanapps.weatherapp.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    // First you check this api whether it is running or not!!
    // API CALL URL ::: https://api.openweathermap.org/data/2.5/weather?lat=33.44&lon=-94.04&appid=4e2d2d137d34778b5b616949c2704a9b

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var activityMainActivity: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainActivity = DataBindingUtil.setContentView(this, R.layout.activity_main)
        supportActionBar?.hide()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        activityMainActivity.rlMainLayout.visibility = View.GONE
        getCurrentLocation();

        activityMainActivity.etGetCityName.setOnEditorActionListener { v, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                getCityWeather(activityMainActivity.etGetCityName.text.toString())
                val view = this.currentFocus
                if (view != null) {

                    val imm: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    activityMainActivity.etGetCityName.clearFocus()
                }
                true

            } else false
        }

        activityMainActivity.refreshButton.setOnClickListener {
            getCurrentLocation()
        }
    }

    private fun getCityWeather(cityName: String) {

        activityMainActivity.pdLoading.visibility = View.VISIBLE
        ApiUtilities.getApiInterface()?.getCityWeatherData(cityName, API_KEY)
            ?.enqueue(object : Callback<ModelClass> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                    setDataOnViews(response.body())
                }

                override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                    // show Error img
                    //activityMainActivity.noConnectionLogo.visibility = View.VISIBLE
                    Toast.makeText(this@MainActivity, "Not a Valid City Name", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    private fun getCurrentLocation() {

        if (checkPermissions()) {

            if (isLocationEnabled()) {

                // final latitude and longitude code here
                if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission()
                    return
                }

                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        Toast.makeText(this, "Null Received", Toast.LENGTH_SHORT).show()
                    } else {
                        // fetch weather here
//                        Toast.makeText(this, "Get Success", Toast.LENGTH_SHORT).show()
                        Toast.makeText(this, "waiting...", Toast.LENGTH_SHORT).show()

                        fetchCurrentLocationWeather(
                            location.latitude.toString(),
                            location.longitude.toString()
                        )

                    }

                }

            } else {
                // setting open here
                Toast.makeText(this, "Turn on location", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }

        } else {
            //request permission here
            requestPermission()
        }
    }

    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {

        activityMainActivity.pdLoading.visibility = View.VISIBLE
        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude, longitude, API_KEY)
            ?.enqueue(object :
                Callback<ModelClass> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {

                    if (response.isSuccessful) {
                        setDataOnViews(response.body())
                    }
                }

                override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                    // show Error img
                    activityMainActivity.refreshLayout.visibility = View.VISIBLE
                    //Toast.makeText(this@MainActivity, "ERROR", Toast.LENGTH_SHORT).show()
                }

            })


    }


    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setDataOnViews(body: ModelClass?) {

        val sdf = SimpleDateFormat("dd/MM/yyyy  hh:mm")
        val currentDate = sdf.format(Date())
        activityMainActivity.tvDateAndTime.text = currentDate

        activityMainActivity.tvDayMaxTemp.text =
            "Day " + kelvinToCelsius(body!!.main.temp_max) + "°"
        activityMainActivity.tvDayMinTemp.text =
            "Night " + kelvinToCelsius(body!!.main.temp_min) + "°"
        activityMainActivity.tvTemp.text = "" + kelvinToCelsius(body!!.main.temp) + "°"
        activityMainActivity.tvFeelsLike.text =
            "Feels Alike " + kelvinToCelsius(body!!.main.feels_like) + "°"
        activityMainActivity.tvWeatherType.text = body.weather[0].main
        activityMainActivity.tvSunrise.text = timeStampToLocalDate(body.sys.sunrise.toLong())
        activityMainActivity.tvSunset.text = timeStampToLocalDate(body.sys.sunset.toLong())
        activityMainActivity.tvPressure.text = body.main.pressure.toString()
        activityMainActivity.tvHumidity.text = body.main.humidity.toString() + " %"
        activityMainActivity.tvWindSpeed.text = body.wind.speed.toString() + " m/s"

        activityMainActivity.tvTempFarenhite.text =
            "" + ((kelvinToCelsius(body.main.temp)).times(1.8).plus(32).roundToInt())

        activityMainActivity.etGetCityName.setText(body.name)

        updateUI(body.weather[0].id)

    }

    private fun kelvinToCelsius(temp: Double): Double {
        var intTemp = temp
        intTemp = intTemp.minus(273)
        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun timeStampToLocalDate(timeStamp: Long): String {
        val localTime = timeStamp.let {
            Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalTime()
        }
        return localTime.toString()
    }

    private fun updateUI(id: Int) {


        if (id in 200..232) {
            //ThunderStorm
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.thunderstorm)
            activityMainActivity.rlToolbar.setBackgroundColor(resources.getColor(R.color.thunderstorm))
            activityMainActivity.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.thunderstrom_bg
            )
            activityMainActivity.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.thunderstrom_bg
            )
            activityMainActivity.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.thunderstrom_bg
            )
            activityMainActivity.ivWeatherBg.setImageResource(R.drawable.thunderstrom_bg)
            activityMainActivity.ivWeatherIcon.setImageResource(R.drawable.thunderstrom)
        } else if (id in 300..321) {

            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.drizzle)
            activityMainActivity.rlToolbar.setBackgroundColor(resources.getColor(R.color.drizzle))
            activityMainActivity.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.drizzle_bg
            )
            activityMainActivity.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.drizzle_bg
            )
            activityMainActivity.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.drizzle_bg
            )
            activityMainActivity.ivWeatherBg.setImageResource(R.drawable.drizzle_bg)
            activityMainActivity.ivWeatherIcon.setImageResource(R.drawable.drizzle)

        } else if (id in 500..531) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.rain)
            activityMainActivity.rlToolbar.setBackgroundColor(resources.getColor(R.color.rain))
            activityMainActivity.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.rainy_bg
            )
            activityMainActivity.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.rainy_bg
            )
            activityMainActivity.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.rainy_bg
            )
            activityMainActivity.ivWeatherBg.setImageResource(R.drawable.rainy_bg)
            activityMainActivity.ivWeatherIcon.setImageResource(R.drawable.rain)

        } else if (id in 600..620) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.snow)
            activityMainActivity.rlToolbar.setBackgroundColor(resources.getColor(R.color.snow))
            activityMainActivity.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.snow_bg
            )
            activityMainActivity.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.snow_bg
            )
            activityMainActivity.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.snow_bg
            )
            activityMainActivity.ivWeatherBg.setImageResource(R.drawable.snow_bg)
            activityMainActivity.ivWeatherIcon.setImageResource(R.drawable.snow)


        } else if (id in 701..781) {

            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.atmosphere)
            activityMainActivity.rlToolbar.setBackgroundColor(resources.getColor(R.color.atmosphere))
            activityMainActivity.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.mist_bg
            )
            activityMainActivity.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.mist_bg
            )
            activityMainActivity.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.mist_bg
            )
            activityMainActivity.ivWeatherBg.setImageResource(R.drawable.mist_bg)
            activityMainActivity.ivWeatherIcon.setImageResource(R.drawable.mist)


        } else if (id == 800) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.clear)
            activityMainActivity.rlToolbar.setBackgroundColor(resources.getColor(R.color.clear))
            activityMainActivity.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.clear_bg
            )
            activityMainActivity.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.clear_bg
            )
            activityMainActivity.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.clear_bg
            )

            activityMainActivity.ivWeatherBg.setImageResource(R.drawable.clear_bg)
            activityMainActivity.ivWeatherIcon.setImageResource(R.drawable.clear)


        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.clouds)
            activityMainActivity.rlToolbar.setBackgroundColor(resources.getColor(R.color.clouds))
            activityMainActivity.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.clouds_bg
            )
            activityMainActivity.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.clouds_bg
            )
            activityMainActivity.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity, R.drawable.clouds_bg
            )

            activityMainActivity.ivWeatherBg.setImageResource(R.drawable.clouds_bg)
            activityMainActivity.ivWeatherIcon.setImageResource(R.drawable.clouds)
        }

        activityMainActivity.pdLoading.visibility = View.GONE
        activityMainActivity.rlMainLayout.visibility = View.VISIBLE


    }


    private fun isLocationEnabled(): Boolean {

        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ), PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
        const val API_KEY = "4e2d2d137d34778b5b616949c2704a9b"
    }

    private fun checkPermissions(): Boolean {

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            return true
        }

        return false

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Granted Start getting the location information
                Toast.makeText(applicationContext, " Granted", Toast.LENGTH_SHORT).show()
                // Permission Image Gone
                activityMainActivity.refreshLayout.visibility = View.GONE
                getCurrentLocation()
            } else {
//                Toast.makeText(applicationContext, "You Deny the Location", Toast.LENGTH_SHORT)
                Toast.makeText(applicationContext, "Please turn on location", Toast.LENGTH_SHORT)
                    .show()
                // Permission Image Show
                activityMainActivity.refreshLayout.visibility = View.VISIBLE
            }
        }
    }


}