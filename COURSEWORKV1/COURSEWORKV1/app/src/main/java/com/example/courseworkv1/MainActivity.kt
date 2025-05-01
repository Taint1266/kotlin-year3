package com.example.courseworkv1


import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*

class MainActivity : AppCompatActivity(){
    private lateinit var etTask: EditText // Input field for tasks
    private lateinit var btnAdd: Button //button for updating the task
    private lateinit var lvTasks: ListView //Diplaying the list of tasks available
    private lateinit var progressBar: ProgressBar // show loading indicator
    private lateinit var tvStatus: TextView // show us the task count

//Reference to the "tasks" node in the Firebase database
    private lateinit var database: DatabaseReference
    private val taskList = mutableListOf<Task>()
    private lateinit var adapter: ArrayAdapter<Task>
    private var selectedTaskId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

            //Initialize UI components
        etTask = findViewById(R.id.etTask)
        btnAdd = findViewById(R.id.btnAdd)
        lvTasks = findViewById(R.id.lvTasks)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)

        //Create a Real-Time Firebase Database.
        database = FirebaseDatabase.getInstance().reference.child("tasks")
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, taskList)
        lvTasks.adapter = adapter

        // Click the button to update an existing task or add a new one.
        btnAdd.setOnClickListener {
            if (selectedTaskId == null){
                createTask()
            }else {
                updateTask()
            }
        }

        // To edit a task, click on it.
        lvTasks.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val task = taskList[position]
            etTask.setText(task.name)
            selectedTaskId = task.id
            btnAdd.text = "Update"
            tvStatus.text = "Editing task..."
        }

        lvTasks.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
            showDeleteDialog(taskList[position].id)
            true
        }

        // Load initial data
        loadTasks()
    }

    // Updates the ListView after retrieving tasks from Firebase.  For real-time updates, a ValueEventListener is used.
    private fun loadTasks() {
        progressBar.visibility = ProgressBar.VISIBLE
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                taskList.clear()
                for (taskSnapshot in snapshot.children) {
                    val task = taskSnapshot.getValue(Task::class.java)
                    task?.let { taskList.add(it) }
                }
                adapter.notifyDataSetChanged()
                progressBar.visibility = ProgressBar.GONE
                tvStatus.text = "${taskList.size} tasks"
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = ProgressBar.GONE
                tvStatus.text = "Error loading tasks"
                Toast.makeText(this@MainActivity, "Failed to load tasks", Toast.LENGTH_SHORT).show()
            }
        })
    }

    //In Firebase, a new task is created.  verifies input and provides comments on progress.

    private fun createTask() {
        val taskText = etTask.text.toString().trim()
        if (taskText.isEmpty()) {
            etTask.error = "Task cannot be empty"
            return
        }

        progressBar.visibility = ProgressBar.VISIBLE
        val taskId = database.push().key
        val task = Task(taskId!!, taskText, false)

        database.child(taskId).setValue(task)
            .addOnSuccessListener {
                etTask.text.clear()
                progressBar.visibility = ProgressBar.GONE
                Toast.makeText(this, "Task added", Toast.LENGTH_SHORT).show()
            }

            .addOnFailureListener {
                progressBar.visibility = ProgressBar.GONE
                Toast.makeText(this, "Failed to add task", Toast.LENGTH_SHORT).show()
            }
    }

    //Modifies an already-existing Firebase task
    // A selectedTaskId is required.
    private fun updateTask() {
        val taskText = etTask.text.toString().trim()
        if (taskText.isEmpty()) {
            etTask.error = "Task cannot be empty"
            return
        }

        selectedTaskId?.let { taskId ->
            progressBar.visibility = ProgressBar.VISIBLE
            database.child(taskId).child("name").setValue(taskText)
                .addOnSuccessListener {
                    resetForm()
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(this, "Task updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(this, "Failed to update task", Toast.LENGTH_SHORT).show()
                }
        }
    }

    //Before deleting a task, a confirmation window is displayed.
    // param taskId  The task's Firebase ID that has to be deleted.

    private fun showDeleteDialog(taskId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Delete") { _, _ -> deleteTask(taskId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //A task is deleted from Firebase
    //@param taskId The task's Firebase ID that has to be deleted.
    private fun deleteTask(taskId: String) {
        progressBar.visibility = ProgressBar.VISIBLE
        database.child(taskId).removeValue()
            .addOnSuccessListener {
                resetForm()
                progressBar.visibility = ProgressBar.GONE
                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                progressBar.visibility = ProgressBar.GONE
                Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show()
            }
    }

    //Returns the user interface to its initial configuration.
    private fun resetForm() {
        etTask.text.clear()
        selectedTaskId = null
        btnAdd.text = "Add"
        tvStatus.text = "${taskList.size} tasks"
    }


    // A task is represented by a data class.
    // Connects to the structure of the Firebase Realtime Database.
    //@property id  unique ID generated by Firebase. Task description for //@property name.
    //@property isCompleted Status of completion (not used in the current user interface).
    data class Task(
        val id: String = "",
        val name: String = "",
        val isCompleted: Boolean = false
    )
}


