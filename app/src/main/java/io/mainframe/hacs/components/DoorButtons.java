package io.mainframe.hacs.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import io.mainframe.hacs.R;
import io.mainframe.hacs.main.Status;

/**
 * Created by holger on 03.10.17.
 */

public class DoorButtons extends LinearLayout implements View.OnClickListener {

    private OnButtonClickListener doorButtonListener;
    private View[] doorButtons;
    private final boolean extendedStatus;

    public DoorButtons(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs,R.styleable.DoorButtons, 0, 0);
        try {
            this.extendedStatus = attributes.getBoolean(R.styleable.DoorButtons_extendedStatus, true);
        } finally {
            attributes.recycle();
        }

        initControl(context);
    }

    private void initControl(Context context) {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        inflater.inflate(R.layout.component_door_buttons, this);

        doorButtons = new View[DoorButton.values().length];
        for (DoorButton doorButton : DoorButton.values()) {
            doorButtons[doorButton.ordinal()] = findViewById(doorButton.getId());
        }

        for (View doorButton : doorButtons) {
            doorButton.setOnClickListener(this);
        }

        if (this.extendedStatus) {
            findViewById(R.id.doorButtons_more).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    findViewById(R.id.doorButtons_moreButtons).setVisibility(View.VISIBLE);
                    findViewById(R.id.doorButtons_more).setVisibility(View.GONE);
                }
            });
        } else {
            findViewById(R.id.doorButtons_more).setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        if (this.doorButtonListener == null) {
            return;
        }

        for (DoorButton doorButton : DoorButton.values()) {
            if (doorButton.getId() == v.getId()) {
                this.doorButtonListener.onClick(doorButton, v);
                return;
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        for (View doorButton : doorButtons) {
            doorButton.setEnabled(enabled);
        }
    }

    public void setOnButtonClickListener(OnButtonClickListener listener) {
        this.doorButtonListener = listener;
    }

    public enum DoorButton {
        OPEN(R.id.doorButtons_open, Status.OPEN),
        CLOSE(R.id.doorButtons_close, Status.CLOSE),
        OPEN_PLUS(R.id.doorButtons_openPlus, Status.OPEN_PLUS),
        KEYHOLDER(R.id.doorButtons_keyholder, Status.KEYHOLDER),
        MEMBER(R.id.doorButtons_member, Status.MEMBER);

        private int id;
        private Status status;
        DoorButton(int id, Status status) {
            this.id = id;
            this.status = status;
        }

        public int getId() {
            return id;
        }

        public Status getStatus() {
            return status;
        }
    }

    public interface OnButtonClickListener {
        void onClick(DoorButton doorButton, View view);
    }
}
