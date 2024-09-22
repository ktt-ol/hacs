package io.mainframe.hacs.PageFragments

import android.app.Activity
import android.arch.lifecycle.Lifecycle
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import io.mainframe.hacs.R
import io.mainframe.hacs.cashbox.Auth
import io.mainframe.hacs.cashbox.CashboxInfo
import io.mainframe.hacs.cashbox.CashboxValueTask
import io.mainframe.hacs.cashbox.EndpointException
import org.pmw.tinylog.Logger
import java.text.SimpleDateFormat
import java.util.*


class CashboxFragment : BasePageFragment() {

    private var cashbox: CashboxInfo? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_cashbox, container, false)

        view.findViewById<ImageButton>(R.id.cashbox_refresh).setOnClickListener {
            loadCashValue(true)
        }
        view.findViewById<Button>(R.id.cashbox_edit).setOnClickListener {
            val ft = activity!!.supportFragmentManager.beginTransaction()
            val dialog = CashboxEditFragment()
            dialog.setTargetFragment(this, CashboxEditFragment.REQ_CODE)
            dialog.show(ft, "editCashbox")
        }


        return view
    }

    override fun onResume() {
        super.onResume()

        loadCashValue(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CashboxEditFragment.REQ_CODE && resultCode == Activity.RESULT_OK) {
            loadCashValue(true)
        }
    }

    private fun loadCashValue(refresh: Boolean) {

        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val user = prefs.getString(context!!.getString(R.string.PREFS_CASHBOX_USER), null)
        val pw = prefs.getString(context!!.getString(R.string.PREFS_CASHBOX_PW), null)
        val cookie = prefs.getString(context!!.getString(R.string.PREFS_CASHBOX_COOKIE), null)

        val cashboxValueView = view!!.findViewById<TextView>(R.id.cashbox_value)
        val cashboxRequestedAtView = view!!.findViewById<TextView>(R.id.cashbox_requestedTime)
        val historyView = view!!.findViewById<LinearLayout>(R.id.cashbox_history)
        val updateView = view!!.findViewById<Button>(R.id.cashbox_edit)

        historyView.removeAllViews()

        if (cashbox != null && !refresh) {
            Logger.debug("Using cached cashbox value.")
            updateUi(cashbox!!, cashboxValueView, cashboxRequestedAtView, historyView, updateView)
            return
        }

        cashboxValueView.text = "..."
        cashboxRequestedAtView.text = "Stand: ?"
        updateView.isEnabled = false
        if (user == null || pw == null) {
            historyView.addView(makeTextView("Es ist kein Benutzer/Passwort gesetzt. Das kannst du in den 'Settings' machen."))
            return
        }

        val networkStatus = interaction.networkStatus
        if (networkStatus.hasMainAreaBssid) {
            historyView.addView(makeTextView("Du bist nicht im Mainframe WLan und in der Hauptfläche."))
            return
        }

        historyView.addView(makeTextView("Lade..."))

        CashboxValueTask(Auth(user, pw, cookie)) { result ->
            historyView.removeAllViews()
            if (result.exception != null) {
                Logger.error("Can't load cashbox.", result.exception)
                if (result.exception is EndpointException) {
                    Logger.info("Error Details:\n$result.exception")
                }
                historyView.addView(makeTextView("Fehler beim Cashbox auslesen: ${result.exception.message}"))
                return@CashboxValueTask
            }

            this.cashbox = result.result

            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                updateUi(cashbox!!, cashboxValueView, cashboxRequestedAtView, historyView, updateView)
            }

            if (result.createdCookie != cookie) {
                Logger.debug("Saving cookie value.")
                prefs.edit().putString(context!!.getString(R.string.PREFS_CASHBOX_COOKIE), result.createdCookie).apply()
            }

        }.execute()
    }

    private fun updateUi(cashbox: CashboxInfo, cashboxValueView: TextView, cashboxRequestedAtView: TextView, historyView: LinearLayout, updateView: Button) {
        cashboxValueView.text = cashbox.value
        val formattedTime = SimpleDateFormat("HH:mm:ss").format(Date(cashbox.requestedAt))
        cashboxRequestedAtView.text = "Stand: ${formattedTime}"

        cashbox.history.forEach {
            historyView.addView(makeTextView("<em>${it.date}</em>: ${it.name}: ${it.amount}"))
        }

        updateView.isEnabled = true
    }

    private fun makeTextView(text: String): TextView {
        val textView = TextView(this.context)
        textView.text = Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
        textView.textSize = 16f
        return textView
    }

    override val titleRes: Int get() = R.string.nav_cashbox
}