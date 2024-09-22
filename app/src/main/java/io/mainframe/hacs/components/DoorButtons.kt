package io.mainframe.hacs.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import io.mainframe.hacs.R
import io.mainframe.hacs.components.DoorButtons.DoorButton
import io.mainframe.hacs.main.Status

typealias ClickListener = (doorButton: DoorButton, view: View) -> Unit

/**
 * Created by holger on 03.10.17.
 */
class DoorButtons(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs),
    View.OnClickListener {
    private var doorButtonListener: ClickListener? = null
    private var doorButtons: List<View> = emptyList()
    private var extendedStatus = false

    init {
        val attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.DoorButtons, 0, 0)
        try {
            this.extendedStatus =
                attributes.getBoolean(R.styleable.DoorButtons_extendedStatus, true)
        } finally {
            attributes.recycle()
        }

        initControl(context)
    }

    private fun initControl(context: Context) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        inflater.inflate(R.layout.component_door_buttons, this)

        if (extendedStatus) {
            // show always all buttons...
            findViewById<View>(R.id.doorButtons_moreButtons).visibility = VISIBLE
            findViewById<View>(R.id.doorButtons_more).visibility = GONE
        }

        doorButtons = DoorButton.entries.map { doorButton ->
            findViewById(doorButton.id)
        }

        for (doorButton in doorButtons) {
            doorButton.setOnClickListener(this)
        }

        if (this.extendedStatus) {
            findViewById<View>(R.id.doorButtons_more).setOnClickListener {
                findViewById<View>(R.id.doorButtons_moreButtons).visibility = VISIBLE
                findViewById<View>(R.id.doorButtons_more).visibility = GONE
            }
        } else {
            findViewById<View>(R.id.doorButtons_more).visibility = INVISIBLE
        }
    }

    override fun onClick(v: View) {
        this.doorButtonListener?.let { listener ->
            for (doorButton in DoorButton.entries) {
                if (doorButton.id == v.id) {
                    listener(doorButton, v)
                    return
                }
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        for (doorButton in doorButtons) {
            doorButton.isEnabled = enabled
        }
    }

    fun setOnButtonClickListener(listener: ClickListener) {
        this.doorButtonListener = listener
    }

    enum class DoorButton(val id: Int, val status: Status) {
        OPEN(R.id.doorButtons_open, Status.OPEN),
        CLOSE(R.id.doorButtons_close, Status.CLOSE),
        OPEN_PLUS(R.id.doorButtons_openPlus, Status.OPEN_PLUS),
        KEYHOLDER(R.id.doorButtons_keyholder, Status.KEYHOLDER),
        MEMBER(R.id.doorButtons_member, Status.MEMBER)
    }

}