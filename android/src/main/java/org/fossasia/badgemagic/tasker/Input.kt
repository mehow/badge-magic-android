package org.fossasia.badgemagic.tasker

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import org.fossasia.badgemagic.R

@TaskerInputRoot
class BadgeMagicInput @JvmOverloads constructor(
    @field:TaskerInputField("text", R.string.text) var text: String? = null,
    @field:TaskerInputField("mode", R.string.mode) var mode: Int = 0,
    @field:TaskerInputField("speed", R.string.speed) var speed: Int = 0
)
