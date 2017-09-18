package com.datpug.androidcore.app

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem

import com.datpug.androidcore.R

import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**
 * @author longv
 * Created on 05-Aug-17.
 */
abstract class AbstractActivity : AppCompatActivity() {

    private var fragments = listOf<Fragment>()
    private val topFragment: Fragment?
        get() = fragments.lastOrNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun <F : Fragment> showFragment (fragmentClass : KClass<F>, @LayoutRes containerId : Int = R.id.fragmentContainer) {
        // Check whether requested fragment needed to be shown is already in stack
        if (fragmentExists(fragmentClass)) {
            throw IllegalStateException("Try to show fragment that is already in stack")
        } else {
            val fragmentTransaction = supportFragmentManager.beginTransaction()

            // Set custom animation for fragment transaction
            // We will want different animations between the first added fragment and the later ones
            if (topFragment == null) fragmentTransaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            else fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)

            if (topFragment != null) fragmentTransaction.hide(topFragment)

            // Show requested fragment
            try {
                val fragment = fragmentClass.createInstance()
                fragmentTransaction.add(R.id.fragmentContainer, fragment)
                fragments = fragments.plus(fragment)
            } catch (e : Resources.NotFoundException) {
                throw RuntimeException("No container found for fragment. Please specified a correct ID for the fragment container or else make sure your layout contains a fragment container having ID: fragmentContainer")
            }

            fragmentTransaction.commit()
        }
    }

    fun closeTopFragment() {
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        // Set custom animation for fragment transaction
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)

        // Remove top fragment
        if (topFragment != null) {
            fragmentTransaction.remove(topFragment)
            fragments = fragments.dropLast(1)
        }
        // Since previous top fragment is hidden when showing a new one, show it again
        if (topFragment != null) fragmentTransaction.show(topFragment)

        fragmentTransaction.commit()
    }

    private fun fragmentTag (position : Int) = "fragment#{$position}"

    fun <F : Fragment> fragmentExists(fragmentClass : KClass<F>) : Boolean {
        fragments.forEach {
            if (fragmentClass.isInstance(it)) return true
        }
        return false
    }

    fun showActivity (activityClass : Class<AppCompatActivity>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    override fun onBackPressed() {
        if ((topFragment as? GeneralAbstractFragment)?.onBackPressed() == true) return

        // End this activity if there is only one fragment left
        if (fragments.size == 1) finish()
        else closeTopFragment()
    }
}