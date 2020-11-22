package org.fossasia.badgemagic.tasker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.fossasia.badgemagic.data.Mode
import org.fossasia.badgemagic.data.Speed
import org.fossasia.badgemagic.util.Converters
import org.fossasia.badgemagic.util.SendingUtils

class TaskerActionRunner : TaskerPluginRunnerActionNoOutput<BadgeMagicInput>() {
    override fun run(context: Context, input: TaskerInput<BadgeMagicInput>): TaskerPluginResult<Unit> {
        if (!Service.isRunning) {
            return TaskerPluginResultError(1, "Not connected")
        }

        val lock = ReentrantLock()
        val done = lock.newCondition()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                val binder = service as Service.LocalBinder
                binder.sendMessage(
                        SendingUtils.convertToDeviceDataModel(
                                org.fossasia.badgemagic.data.Message(
                                        Converters.convertTextToLEDHex(
                                                input.regular.text ?: " ",
                                                false
                                        ).second,
                                        flash = input.regular.mode == 3,
                                        marquee = false,
                                        speed = Speed.values()[input.regular.speed],
                                        mode = getMode(input.regular.mode)
                                ))) {
                    context.unbindService(this)
                    lock.withLock {
                        done.signal()
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                System.out.println("disconnected")
            }
        }
        val intent = Intent(context, Service::class.java)
        if (!context.bindService(intent, connection, 0)) {
            return TaskerPluginResultError(1, "Not connected")
        }
        lock.withLock {
            done.await()
        }
        return TaskerPluginResultSucess()
    }

    fun getMode(i: Int): Mode {
        return when (i) {
            1 -> Mode.RIGHT
            2 -> Mode.LEFT
            else -> Mode.FIXED
        }
    }
}
