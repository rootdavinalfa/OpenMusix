package xyz.dvnlabs.openmusix.ui.fragment

import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import xyz.dvnlabs.openmusix.R

open class FragmentHost : Fragment() {
    private val appBarConfig = AppBarConfiguration(setOf(R.id.fragmentLibrary))
    private lateinit var toolbar: Toolbar

    override fun onStart() {
        super.onStart()
        // setup navigation with toolbar
        toolbar = requireActivity().findViewById(R.id.masterToolbar)
        val navController = requireActivity().findNavController(R.id.navigationHost)
        visibilityNavElements(navController)
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfig)
    }

    private fun visibilityNavElements(navController: NavController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.fragmentLibrary -> toolbar.visibility = View.GONE
                R.id.fragmentSearch -> toolbar.visibility = View.GONE
                R.id.fragmentPlayer -> toolbar.visibility = View.GONE
                else -> toolbar.visibility = View.VISIBLE
            }
        }
    }

    open fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
}