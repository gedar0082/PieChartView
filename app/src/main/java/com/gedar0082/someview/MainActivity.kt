package com.gedar0082.someview

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val chartView : ChartView = findViewById(R.id.my_chart)

        chartView.data = (doubleArrayOf(1.0, 1.0, 2.0, 3.0, 5.0, 8.0, 13.0))
        chartView.colors = intArrayOf(Color.BLACK, Color.CYAN, Color.BLUE, Color.DKGRAY, Color.GRAY, Color.GREEN)
        chartView.outlineWidth =100f
        chartView.outlineColor = Color.BLUE

    }


}