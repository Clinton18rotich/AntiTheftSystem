package com.antitheft.app.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antitheft.app.R
import com.antitheft.app.managers.CommandParseResult
import com.antitheft.app.managers.CustomCommand
import com.antitheft.app.managers.CustomCommandManager

class CommandConfigurationActivity : AppCompatActivity() {
    
    private lateinit var commandManager: CustomCommandManager
    private lateinit var commandInput: EditText
    private lateinit var parseButton: Button
    private lateinit var previewText: TextView
    private lateinit var commandsRecyclerView: RecyclerView
    private lateinit var templateSpinner: Spinner
    
    private val adapter = CommandAdapter { command ->
        AlertDialog.Builder(this)
            .setTitle("Command: ${command.name}")
            .setMessage("Trigger: ${command.trigger}\nActions: ${command.actions.size}")
            .setPositiveButton("Delete") { _, _ ->
                commandManager.removeCustomCommand(command.trigger)
                loadCommands()
            }
            .setNegativeButton("Close", null).show()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_command_configuration)
        
        commandManager = CustomCommandManager(this)
        commandInput = findViewById(R.id.commandInput)
        parseButton = findViewById(R.id.parseButton)
        previewText = findViewById(R.id.previewText)
        commandsRecyclerView = findViewById(R.id.commandsRecyclerView)
        templateSpinner = findViewById(R.id.templateSpinner)
        
        commandsRecyclerView.layoutManager = LinearLayoutManager(this)
        commandsRecyclerView.adapter = adapter
        
        val templates = listOf(
            "Select template..." to "",
            "Quick Video (2 sec)" to "TRIGGER: vid\nNAME: Quick Video\nACTION: RECORD_VIDEO duration=2",
            "Emergency" to "TRIGGER: sos\nNAME: Emergency\nACTION: LOCK_DEVICE\nACTION: GET_LOCATION\nACTION: RECORD_AUDIO duration=30"
        )
        templateSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, templates.map { it.first }).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        templateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos > 0) commandInput.setText(templates[pos].second)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        
        parseButton.setOnClickListener {
            when (val result = commandManager.addCustomCommand(commandInput.text.toString())) {
                is CommandParseResult.Success -> {
                    Toast.makeText(this, "Command added!", Toast.LENGTH_SHORT).show()
                    commandInput.text.clear()
                    loadCommands()
                }
                is CommandParseResult.Error -> Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.helpButton).setOnClickListener {
            AlertDialog.Builder(this).setTitle("Help")
                .setMessage("TRIGGER: [word]\nNAME: [name]\nACTION: [type] [params]\n\nActions: RECORD_VIDEO, TAKE_PHOTO, GET_LOCATION, LOCK_DEVICE, VIBRATE, FLASHLIGHT, VOLUME, WIFI, BLUETOOTH, RECORD_AUDIO")
                .setPositiveButton("OK", null).show()
        }
        
        commandInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrBlank()) previewText.text = "Enter command above..."
                else previewText.text = "Ready to parse"
            }
        })
        
        loadCommands()
    }
    
    private fun loadCommands() {
        adapter.submitList(commandManager.getAllCustomCommands())
    }
    
    inner class CommandAdapter(private val onItemClick: (CustomCommand) -> Unit) :
        RecyclerView.Adapter<CommandAdapter.ViewHolder>() {
        
        private var commands = listOf<CustomCommand>()
        
        fun submitList(list: List<CustomCommand>) {
            commands = list.sortedByDescending { it.createdAt }
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_command, parent, false)
        )
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(commands[position])
        
        override fun getItemCount() = commands.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val triggerText: TextView = itemView.findViewById(R.id.triggerText)
            private val nameText: TextView = itemView.findViewById(R.id.nameText)
            private val actionsText: TextView = itemView.findViewById(R.id.actionsText)
            
            fun bind(command: CustomCommand) {
                triggerText.text = "Trigger: ${command.trigger}"
                nameText.text = command.name
                actionsText.text = "${command.actions.size} action(s)"
                itemView.setOnClickListener { onItemClick(command) }
                itemView.findViewById<ImageButton>(R.id.deleteButton).setOnClickListener { onItemClick(command) }
            }
        }
    }
}
