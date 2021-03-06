package com.example.btcpricewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

/**
 * Implementation of App Widget functionality.
 */

// global variable to hold data from GET request
var data = Data()

class PriceWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {

    // create remote view
    val views = RemoteViews(context.packageName, R.layout.price_widget)

    views.setTextViewText(R.id.appwidget_text, "Loading...")
    views.setTextViewText(R.id.day_change, "Loading...")
    views.setTextColor(R.id.day_change, Color.BLACK)

    println("Homescreen Widget: Refresh button pressed!")
    // first update call to set loading text
    appWidgetManager.updateAppWidget(appWidgetId, views) // continues after this

    // refresh button
    val intentUpdate = Intent(context, PriceWidget::class.java)
    intentUpdate.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

    val idArray = intArrayOf(appWidgetId)
    intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idArray)

    val pendingUpdate = PendingIntent.getBroadcast(context, appWidgetId, intentUpdate, PendingIntent.FLAG_UPDATE_CURRENT)
    views.setOnClickPendingIntent(R.id.refresh_button, pendingUpdate)

    // function call to fetch data from HTTP GET request
    fetchData(appWidgetManager, appWidgetId, views)
}

fun fetchData(appWidgetManager: AppWidgetManager, appWidgetId: Int, views: RemoteViews) {

    val url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=bitcoin"
    val request = Request.Builder().url(url).build()

    val client = OkHttpClient()

    client.newCall(request).enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            println("Homescreen Widget: GET request successful.")

            // converts response into string
            val body = response.body?.string()

            // extracts object from JSON
            val tempList: Array<Data> = Gson().fromJson(body, Array<Data>::class.java)
            data = tempList[0]

            // post execute here
            // update widget with new data
            views.setTextViewText(R.id.appwidget_text, data.price())
            views.setTextViewText(R.id.day_change, data.change24h())

            if (data.change24h().contains('+')) {
                // green color
                views.setTextColor(R.id.day_change, Color.parseColor("#1f6d00"))
            } else {
                // red color
                views.setTextColor(R.id.day_change, Color.parseColor("#b50f04"))
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)

        }

        override fun onFailure(call: Call, e: IOException) {
            println("Homescreen Widget: Failed to execute GET request.")
        }
    })
}