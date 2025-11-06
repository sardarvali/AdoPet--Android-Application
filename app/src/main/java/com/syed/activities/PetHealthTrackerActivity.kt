package com.syed.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.syed.R
import com.syed.adapters.HealthRecordsAdapter
import com.syed.models.HealthRecord
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class PetHealthTrackerActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HealthRecordsAdapter
    private lateinit var addRecordFab: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView

    private val firestore = FirebaseFirestore.getInstance()
    private val healthRecords = mutableListOf<HealthRecord>()
    private var petId: String? = null
    private var petName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_health_tracker)

        petId = intent.getStringExtra("petId")
        petName = intent.getStringExtra("petName") ?: "Pet"

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "$petName - Health Records"

        initializeViews()
        setupRecyclerView()
        loadHealthRecords()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.healthRecordsRecyclerView)
        addRecordFab = findViewById(R.id.addRecordFab)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.emptyView)

        addRecordFab.setOnClickListener {
            showAddRecordDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter =
            HealthRecordsAdapter(
                healthRecords,
                onEditClick = { record -> showEditRecordDialog(record) },
                onDeleteClick = { record -> deleteHealthRecord(record) },
            )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadHealthRecords() {
        if (petId == null) {
            Toast.makeText(this, "Error: Pet ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val snapshot =
                    firestore
                        .collection("pets")
                        .document(petId!!)
                        .collection("healthRecords")
                        .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .get()
                        .await()

                healthRecords.clear()
                for (doc in snapshot.documents) {
                    val record = doc.toObject(HealthRecord::class.java)
                    record?.let {
                        it.id = doc.id
                        healthRecords.add(it)
                    }
                }

                adapter.notifyDataSetChanged()
                updateEmptyView()
                progressBar.visibility = View.GONE
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast
                    .makeText(
                        this@PetHealthTrackerActivity,
                        "Error loading records: ${e.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }
    }

    private fun showAddRecordDialog() {
        val dialogView =
            LayoutInflater
                .from(this)
                .inflate(R.layout.dialog_add_health_record, null)

        val typeSpinner = dialogView.findViewById<Spinner>(R.id.typeSpinner)
        val titleInput = dialogView.findViewById<EditText>(R.id.titleInput)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.descriptionInput)
        val dateButton = dialogView.findViewById<Button>(R.id.dateButton)
        val nextDueDateButton = dialogView.findViewById<Button>(R.id.nextDueDateButton)
        val vetNameInput = dialogView.findViewById<EditText>(R.id.vetNameInput)
        val costInput = dialogView.findViewById<EditText>(R.id.costInput)
        val notesInput = dialogView.findViewById<EditText>(R.id.notesInput)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        var selectedDate = calendar.timeInMillis
        var nextDueDate: Long? = null

        dateButton.text = dateFormat.format(calendar.time)
        dateButton.setOnClickListener {
            showDatePicker(calendar) { date ->
                selectedDate = date
                dateButton.text = dateFormat.format(Date(date))
            }
        }

        nextDueDateButton.setOnClickListener {
            showDatePicker(calendar) { date ->
                nextDueDate = date
                nextDueDateButton.text = dateFormat.format(Date(date))
            }
        }

        // Setup type spinner
        val types = arrayOf("Vaccination", "Checkup", "Surgery", "Medication", "Lab Test", "Dental", "Other")
        typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)

        AlertDialog
            .Builder(this)
            .setTitle("Add Health Record")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val record =
                    HealthRecord(
                        type = typeSpinner.selectedItem.toString(),
                        title = titleInput.text.toString(),
                        description = descriptionInput.text.toString(),
                        date = selectedDate,
                        nextDueDate = nextDueDate,
                        vetName = vetNameInput.text.toString(),
                        cost = costInput.text.toString().toDoubleOrNull() ?: 0.0,
                        notes = notesInput.text.toString(),
                    )
                addHealthRecord(record)
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditRecordDialog(record: HealthRecord) {
        val dialogView =
            LayoutInflater
                .from(this)
                .inflate(R.layout.dialog_add_health_record, null)

        val typeSpinner = dialogView.findViewById<Spinner>(R.id.typeSpinner)
        val titleInput = dialogView.findViewById<EditText>(R.id.titleInput)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.descriptionInput)
        val dateButton = dialogView.findViewById<Button>(R.id.dateButton)
        val nextDueDateButton = dialogView.findViewById<Button>(R.id.nextDueDateButton)
        val vetNameInput = dialogView.findViewById<EditText>(R.id.vetNameInput)
        val costInput = dialogView.findViewById<EditText>(R.id.costInput)
        val notesInput = dialogView.findViewById<EditText>(R.id.notesInput)

        // Pre-fill with existing data
        titleInput.setText(record.title)
        descriptionInput.setText(record.description)
        vetNameInput.setText(record.vetName)
        costInput.setText(record.cost.toString())
        notesInput.setText(record.notes)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        dateButton.text = dateFormat.format(Date(record.date))
        record.nextDueDate?.let {
            nextDueDateButton.text = dateFormat.format(Date(it))
        }

        val types = arrayOf("Vaccination", "Checkup", "Surgery", "Medication", "Lab Test", "Dental", "Other")
        typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        typeSpinner.setSelection(types.indexOf(record.type))

        var selectedDate = record.date
        var nextDueDate = record.nextDueDate
        val calendar = Calendar.getInstance()

        dateButton.setOnClickListener {
            showDatePicker(calendar) { date ->
                selectedDate = date
                dateButton.text = dateFormat.format(Date(date))
            }
        }

        nextDueDateButton.setOnClickListener {
            showDatePicker(calendar) { date ->
                nextDueDate = date
                nextDueDateButton.text = dateFormat.format(Date(date))
            }
        }

        AlertDialog
            .Builder(this)
            .setTitle("Edit Health Record")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val updatedRecord =
                    record.copy(
                        type = typeSpinner.selectedItem.toString(),
                        title = titleInput.text.toString(),
                        description = descriptionInput.text.toString(),
                        date = selectedDate,
                        nextDueDate = nextDueDate,
                        vetName = vetNameInput.text.toString(),
                        cost = costInput.text.toString().toDoubleOrNull() ?: 0.0,
                        notes = notesInput.text.toString(),
                    )
                updateHealthRecord(updatedRecord)
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatePicker(
        calendar: Calendar,
        onDateSelected: (Long) -> Unit,
    ) {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun addHealthRecord(record: HealthRecord) {
        if (petId == null) return

        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                firestore
                    .collection("pets")
                    .document(petId!!)
                    .collection("healthRecords")
                    .add(record)
                    .await()

                Toast.makeText(this@PetHealthTrackerActivity, "Record added successfully", Toast.LENGTH_SHORT).show()
                loadHealthRecords()
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast
                    .makeText(
                        this@PetHealthTrackerActivity,
                        "Error adding record: ${e.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }
    }

    private fun updateHealthRecord(record: HealthRecord) {
        if (petId == null || record.id.isEmpty()) return

        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                firestore
                    .collection("pets")
                    .document(petId!!)
                    .collection("healthRecords")
                    .document(record.id)
                    .set(record)
                    .await()

                Toast.makeText(this@PetHealthTrackerActivity, "Record updated successfully", Toast.LENGTH_SHORT).show()
                loadHealthRecords()
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast
                    .makeText(
                        this@PetHealthTrackerActivity,
                        "Error updating record: ${e.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }
    }

    private fun deleteHealthRecord(record: HealthRecord) {
        if (petId == null || record.id.isEmpty()) return

        AlertDialog
            .Builder(this)
            .setTitle("Delete Record")
            .setMessage("Are you sure you want to delete this health record?")
            .setPositiveButton("Delete") { _, _ ->
                progressBar.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        firestore
                            .collection("pets")
                            .document(petId!!)
                            .collection("healthRecords")
                            .document(record.id)
                            .delete()
                            .await()

                        Toast.makeText(this@PetHealthTrackerActivity, "Record deleted", Toast.LENGTH_SHORT).show()
                        loadHealthRecords()
                    } catch (e: Exception) {
                        progressBar.visibility = View.GONE
                        Toast
                            .makeText(
                                this@PetHealthTrackerActivity,
                                "Error deleting record: ${e.message}",
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                }
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateEmptyView() {
        if (healthRecords.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
