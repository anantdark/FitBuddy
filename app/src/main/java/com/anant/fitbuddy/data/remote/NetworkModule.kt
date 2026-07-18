package com.anant.fitbuddy.data.remote

import com.anant.fitbuddy.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/** Lightweight manual DI for the network graph (no Hilt/Dagger to keep the sample self-contained). */
object NetworkModule {

    // Placeholder base; every call supplies an absolute @Url resolved from user settings.
    private const val PLACEHOLDER_BASE_URL = "https://openrouter.ai/api/v1/"

    /** OFF asks for a custom UA so clients aren't treated as anonymous bots. */
    private val openFoodFactsUserAgent =
        "FitBuddy/${BuildConfig.VERSION_NAME} (Android; https://github.com/anantdark/FitBuddy)"

    val moshi: Moshi by lazy {
        Moshi.Builder()
            // Round fractional JSON numbers into Int fields (backup macros, AI payloads).
            .add(CoercingIntJsonAdapter.FACTORY)
            // Codegen adapters are used for our own DTOs; the reflect factory is a safety net
            // for anything not annotated with @JsonClass.
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val openFoodFactsClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", openFoodFactsUserAgent)
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            })
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(PLACEHOLDER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun provideAiApi(): AiApi = retrofit.create(AiApi::class.java)

    private val openFoodFactsRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(openFoodFactsClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun provideOpenFoodFactsApi(): OpenFoodFactsApi = openFoodFactsRetrofit.create(OpenFoodFactsApi::class.java)

    private val githubRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun provideGithubApi(): GithubApi = githubRetrofit.create(GithubApi::class.java)
}
