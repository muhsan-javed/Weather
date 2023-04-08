package com.muhsanapps.weatherapp.Utilities

import com.muhsanapps.weatherapp.Models.ModelClass
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {

    @GET("weather")
    fun getCurrentWeatherData(
        @Query("lat") latitude: String,
        @Query("lon") longitude: String,
        @Query("AppId") api_Key: String
    ):Call<ModelClass>


    @GET("weather")
    fun getCityWeatherData(
        @Query("q") cityName: String,
        @Query("AppId") api_Key: String
    ):Call<ModelClass>
}