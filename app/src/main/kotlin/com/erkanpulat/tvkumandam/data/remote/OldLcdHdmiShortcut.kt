package com.erkanpulat.tvkumandam.data.remote

import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteSequence
import com.erkanpulat.tvkumandam.domain.model.RemoteSequenceStep

/** Reliable RC5 navigation sequences for the old LCD source menu. */
internal object OldLcdHdmiShortcut {
    private const val HDMI_DOWN_PRESS_COUNT = 7
    private const val SOURCE_MENU_OPEN_DELAY_MILLIS = 1_000L
    private const val MENU_NAVIGATION_DELAY_MILLIS = 300L
    private const val SELECTION_SETTLE_DELAY_MILLIS = 500L
    private const val RC5_FRAMES_PER_PRESS = 2
    private const val RC5_INTER_FRAME_GAP_MILLIS = 89L

    val openAndSelectHdmi1 = RemoteSequence(
        buildList {
            add(step(RemoteCommand.SOURCE, SOURCE_MENU_OPEN_DELAY_MILLIS))
            repeat(HDMI_DOWN_PRESS_COUNT) { index ->
                val delayAfterMillis = if (index == HDMI_DOWN_PRESS_COUNT - 1) {
                    SELECTION_SETTLE_DELAY_MILLIS
                } else {
                    MENU_NAVIGATION_DELAY_MILLIS
                }
                add(step(RemoteCommand.DOWN, delayAfterMillis))
            }
            add(step(RemoteCommand.OK))
        },
    )
    private fun step(
        command: RemoteCommand,
        delayAfterMillis: Long = 0L,
    ) = RemoteSequenceStep(
        command = command,
        delayAfterMillis = delayAfterMillis,
        transmissionCount = RC5_FRAMES_PER_PRESS,
        transmissionGapMillis = RC5_INTER_FRAME_GAP_MILLIS,
    )
}
