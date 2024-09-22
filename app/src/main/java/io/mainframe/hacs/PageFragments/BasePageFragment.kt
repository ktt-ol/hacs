package io.mainframe.hacs.PageFragments

import android.content.Context
import android.support.v4.app.Fragment
import io.mainframe.hacs.common.Constants.DoorServer
import io.mainframe.hacs.main.NetworkStatus
import io.mainframe.hacs.ssh.DoorCommand
import io.mainframe.hacs.status.SpaceStatusService
import io.mainframe.hacs.trash_notifications.TrashCalendar

/**
 * Created by holger on 06.10.17.
 */
abstract class BasePageFragment : Fragment() {
    private var _interaction: BasePageFragmentInteractionListener? = null

    abstract val titleRes: Int

    val interaction: BasePageFragmentInteractionListener get() = checkNotNull(_interaction)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BasePageFragmentInteractionListener) {
            _interaction = context
        } else {
            throw RuntimeException("$context must implement BasePageFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        _interaction = null
    }

    interface BasePageFragmentInteractionListener {
        val networkStatus: NetworkStatus
        val statusService: SpaceStatusService
        val trashCalendar: TrashCalendar
        fun sendSshCommand(server: DoorServer, command: DoorCommand)
        fun navigateToPage(target: Class<out BasePageFragment>)
    }
}
