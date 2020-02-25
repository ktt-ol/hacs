package io.mainframe.hacs.PageFragments

import android.app.Activity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.mainframe.hacs.R
import io.mainframe.hacs.cashbox.Auth
import io.mainframe.hacs.cashbox.UpdateCashboxTask
import io.mainframe.hacs.cashbox.UpdateParam
import io.mainframe.hacs.cashbox.UpdateType
import org.pmw.tinylog.Logger

class CashboxEditFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_cashbox_edit, container, false)

        val spinner = view.findViewById<Spinner>(R.id.cashbox_edit_action)
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
                context!!,
                R.array.cashbox_edit_array,
                android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }

        view.findViewById<Button>(R.id.cashbox_edit_add).setOnClickListener {
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            val user = prefs.getString(context!!.getString(R.string.PREFS_CASHBOX_USER), null)!!
            val pw = prefs.getString(context!!.getString(R.string.PREFS_CASHBOX_PW), null)!!
            val cookie = prefs.getString(context!!.getString(R.string.PREFS_CASHBOX_COOKIE), null)

            val type = UpdateType.values()[spinner.selectedItemPosition]
            val amount = view!!.findViewById<EditText>(R.id.cashbox_edit_amount).text.toString().toInt()

            val params = UpdateParam(type, amount)
            Logger.debug("Update cashbox with: ${params}")


            controlsEnabled(false)

            UpdateCashboxTask(params, Auth(user, pw, cookie)) { excp, ok, createdCookie ->
                if (createdCookie != null) {
                    Logger.debug("Saving cookie value.")
                    prefs.edit().putString(context!!.getString(R.string.PREFS_CASHBOX_COOKIE), createdCookie).apply()
                }

                if (ok == true) {
                    Logger.info("Cashbox update successful.")
                    targetFragment!!.onActivityResult(REQ_CODE, Activity.RESULT_OK, activity!!.intent)
                    dismiss()
                    return@UpdateCashboxTask
                }

                view.findViewById<TextView>(R.id.cashbox_error_msg).also {
                    it.text = excp?.message ?: "Unbekannter Fehler :("
                    it.visibility = View.VISIBLE
                }
                controlsEnabled(true)
            }.execute()
        }

        return view
    }


    private fun controlsEnabled(isEnabled: Boolean) {
        val controls = arrayOf(R.id.cashbox_edit_action, R.id.cashbox_edit_amount, R.id.cashbox_edit_add)
        controls.forEach { view!!.findViewById<View>(it).isEnabled = isEnabled }

        view!!.findViewById<Button>(R.id.cashbox_edit_add).text = if (isEnabled) "Hinzufügen" else "..."
    }

    companion object {
        const val REQ_CODE: Int = 34590345
    }
}