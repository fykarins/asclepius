package com.dicoding.asclepius.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.asclepius.R
import com.dicoding.asclepius.adapter.HistoryAdapter
import com.dicoding.asclepius.adapter.PredictionDiffCallback
import com.dicoding.asclepius.data.local.AsclepiusDatabase
import com.dicoding.asclepius.data.local.PredictionHistory
import com.dicoding.asclepius.view.ResultActivity.Companion.REQUEST_HISTORY_UPDATE
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity(), HistoryAdapter.OnDeleteClickListener {

    private lateinit var predictionRecyclerView: RecyclerView
    private lateinit var predictionAdapter: HistoryAdapter
    private var predictionList: MutableList<PredictionHistory> = mutableListOf()
    private lateinit var tvNotFound: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var historyUpdateLauncher: ActivityResultLauncher<Intent>

    companion object{
        const val TAG = "historydata"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        historyUpdateLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                lifecycleScope.launch(Dispatchers.Main) {
                    loadPredictionHistoryFromDatabase()
                }
            }
        }

        bottomNavigationView = findViewById(R.id.menuBar)
        predictionRecyclerView = findViewById(R.id.rvHistory)
        tvNotFound = findViewById(R.id.tvNotFound)

        bottomNavigationView.selectedItemId = R.id.history_menu
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.news -> {
                    startActivity(Intent(this, NewsActivity::class.java))
                    finish()
                    true
                }
                R.id.history_menu -> {
                    true
                }
                else -> false
            }
        }
        predictionRecyclerView = findViewById(R.id.rvHistory)
        tvNotFound = findViewById(R.id.tvNotFound)

        predictionAdapter = HistoryAdapter(predictionList)
        predictionAdapter.setOnDeleteClickListener(this)
        predictionRecyclerView.adapter = predictionAdapter
        predictionRecyclerView.layoutManager = LinearLayoutManager(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lifecycleScope.launch(Dispatchers.Main) {
            loadPredictionHistoryFromDatabase()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_HISTORY_UPDATE && resultCode == RESULT_OK) {
            lifecycleScope.launch(Dispatchers.Main) {
                loadPredictionHistoryFromDatabase()
            }
        }
    }

    private fun loadPredictionHistoryFromDatabase() {
        lifecycleScope.launch(Dispatchers.Main) {
            val predictions = AsclepiusDatabase.getDatabase(this@HistoryActivity).predictionHistoryDao().getAllPredictions()
            Log.d(TAG, "Number of predictions: ${predictions.size}")

            val diffCallback = PredictionDiffCallback(predictionList, predictions)
            val diffResult = DiffUtil.calculateDiff(diffCallback)

            predictionList.clear()
            predictionList.addAll(predictions)
            diffResult.dispatchUpdatesTo(predictionAdapter)
            showOrHideNoHistoryText()
        }
    }

    private fun showOrHideNoHistoryText() {
        if (predictionList.isEmpty()) {
            tvNotFound.visibility = View.VISIBLE
            predictionRecyclerView.visibility = View.GONE
        } else {
            tvNotFound.visibility = View.GONE
            predictionRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDeleteClick(position: Int) {
        val prediction = predictionList[position]
        if (prediction.result.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                AsclepiusDatabase.getDatabase(this@HistoryActivity).predictionHistoryDao().deletePrediction(prediction)
            }
            predictionList.removeAt(position)
            predictionAdapter.notifyItemRemoved(position)
            showOrHideNoHistoryText()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}