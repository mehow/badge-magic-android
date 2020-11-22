package org.fossasia.badgemagic.tasker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.android.synthetic.main.activity_tasker_config.*
import org.fossasia.badgemagic.R
import org.fossasia.badgemagic.core.bluetooth.GattClient
import org.fossasia.badgemagic.core.bluetooth.ScanHelper
import org.fossasia.badgemagic.data.DataToSend
import org.fossasia.badgemagic.ui.TaskerActionActivity
import org.fossasia.badgemagic.ui.toggleTaskerState

class Service : android.app.Service() {

    companion object {
        var isRunning = false
    }

    enum class State {
        DISCONNECTED,
        SCANNING,
        CONNECTING,
        CONNECTED
    }

    private val binder = LocalBinder()
    private val scanHelper = ScanHelper()
    private val gattClient = GattClient()
    private val stateListeners = ArrayList<StateListener>()
    private var state: State = State.DISCONNECTED

    override fun onCreate() {
        isRunning = true
        updateState(State.SCANNING)
        scanHelper.startLeScan { device ->
            if (device == null) {
                Toast.makeText(this, R.string.no_device_found, Toast.LENGTH_SHORT).show()
                stopSelf()
            } else {
                updateState(State.CONNECTING)
                gattClient.startClient(this, device.address) { onConnected ->
                    if (!onConnected) {
                        stopSelf()
                    } else {
                        updateState(State.CONNECTED)
                        toggleTaskerState()
                        startInForeground()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // Do not restart automatically
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onDestroy() {
        if (this.state == State.CONNECTED) {
            toggleTaskerState()
        }
        updateState(State.DISCONNECTED)
        isRunning = false
        stateListeners.clear()
        scanHelper.stopLeScan()
        gattClient.stopClient()
    }

    private fun startInForeground() {
        val channelId =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel("badge_magic", "Badge Magic")
                } else {
                    // If earlier version channel ID is not used
                    // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                    ""
                }

        val pendingIntent: PendingIntent =
                Intent(this, TaskerActionActivity::class.java).let { notificationIntent ->
                    PendingIntent.getActivity(this, 0, notificationIntent, 0)
                }

        val notificationBuilder =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    Notification.Builder(this, channelId)
                } else {
                    Notification.Builder(this)
                }
        val notification = notificationBuilder
                .setContentTitle(getText(R.string.app_name))
                .setContentText("Badge connected")
                .setSmallIcon(R.drawable.ic_star)
                .setContentIntent(pendingIntent)
                .build()

        startForeground(123, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private fun updateState(state: State) {
        this.state = state
        stateListeners.forEach { listener -> listener.onStateChange(state) }
    }

    inner class LocalBinder : Binder() {
        fun addStateListener(listener: StateListener) {
            stateListeners.add(listener)
            listener.onStateChange(state)
        }
        fun sendMessage(dataToSend: DataToSend, onFinishWritingDataListener: () -> Unit) {
            val byteData = gattClient.convert(dataToSend)
            gattClient.writeDataStart(byteData, onFinishWritingDataListener)
        }
    }

    abstract interface StateListener {
        abstract fun onStateChange(state: State)
    }
}
