package org.fossasia.badgemagic.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import kotlinx.android.synthetic.main.activity_tasker_config.*
import org.fossasia.badgemagic.R
import org.fossasia.badgemagic.tasker.BadgeMagicInput
import org.fossasia.badgemagic.tasker.Service
import org.fossasia.badgemagic.tasker.Service.StateListener
import org.fossasia.badgemagic.tasker.TaskerActionRunner

class TaskerActionHelper(config: TaskerPluginConfig<BadgeMagicInput>) : TaskerPluginConfigHelperNoOutput<BadgeMagicInput, TaskerActionRunner>(config) {
    override val inputClass = BadgeMagicInput::class.java
    override val runnerClass: Class<TaskerActionRunner> get() = TaskerActionRunner::class.java
    override val addDefaultStringBlurb = false
    override fun addToStringBlurb(input: TaskerInput<BadgeMagicInput>, blurbBuilder: StringBuilder) {
        blurbBuilder.append("\"" + input.regular.text + "\" (")
        when (input.regular.mode) {
            0 -> blurbBuilder.append("fixed")
            1 -> blurbBuilder.append("left to right")
            2 -> blurbBuilder.append("right to left")
            3 -> blurbBuilder.append("blink")
        }
        if (input.regular.mode == 1 || input.regular.mode == 2) {
            blurbBuilder.append(", ")
            when (input.regular.speed) {
                0 -> blurbBuilder.append("slow")
                1 -> blurbBuilder.append("normal")
                2 -> blurbBuilder.append("fast")
            }
        }
        blurbBuilder.append(")")
    }
}

class TaskerActionActivity : Activity(), TaskerPluginConfig<BadgeMagicInput>, View.OnClickListener {
    override val context get() = applicationContext
    private val taskerHelper by lazy { TaskerActionHelper(this) }
    private var connected: Boolean = false
    private var serviceIntent: Intent? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            connected = true
            val binder = service as Service.LocalBinder
            val listener = object : StateListener {
                override fun onStateChange(state: Service.State) {
                    buttonConnect.post {
                        when (state) {
                            Service.State.DISCONNECTED -> {
                                buttonConnect.text = "Connect"
                                buttonConnect.isEnabled = true
                            }
                            Service.State.SCANNING -> buttonConnect.text = "Scanning..."
                            Service.State.CONNECTING -> buttonConnect.text = "Connecting..."
                            Service.State.CONNECTED -> {
                                buttonConnect.text = "Disconnect"
                                buttonConnect.isEnabled = true
                            }
                        }
                    }
                }
            }
            binder.addStateListener(listener)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            connected = false
            unbindService(this)
        }
    }

    override fun assignFromInput(input: TaskerInput<BadgeMagicInput>) = input.regular.run {
        editText.setText(text)
        when (mode) {
            0 -> radioFixed.isChecked = true
            1 -> radioLeftToRight.isChecked = true
            2 -> radioRightToLeft.isChecked = true
            3 -> radioBlink.isChecked = true
        }
        when (speed) {
            0 -> radioSlow.isChecked = true
            1 -> radioNormal.isChecked = true
            2 -> radioFast.isChecked = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasker_config)
        taskerHelper.onCreate()
        serviceIntent = Intent(this, Service::class.java)
        if (Service.isRunning) {
            bindService()
        }
        buttonConnect.setOnClickListener(this)
    }

    override fun onBackPressed() {
        taskerHelper.onBackPressed()
    }

    override val inputForTasker get() = TaskerInput(BadgeMagicInput(
            editText.text?.toString(), getMode(), getSpeed()))

    private fun getMode(): Int {
        return when {
            radioLeftToRight.isChecked -> 1
            radioRightToLeft.isChecked -> 2
            radioBlink.isChecked -> 3
            else -> 0
        }
    }

    private fun getSpeed(): Int {
        return when {
            radioNormal.isChecked -> 1
            radioFast.isChecked -> 2
            else -> 0
        }
    }

    override fun onClick(v: View?) {
        if (connected) {
            stopService(serviceIntent)
        } else {
            startService(serviceIntent)
            bindService()
        }
    }

    private fun bindService() {
        bindService(serviceIntent, connection, 0)
        buttonConnect.isEnabled = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (connected) {
            connected = false
            unbindService(connection)
        }
    }
}
