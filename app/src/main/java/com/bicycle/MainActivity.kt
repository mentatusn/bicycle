package com.bicycle


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bicycle.databinding.ActivityMainBinding

import com.bicycle.util.Const
import com.bicycle.view.MainFragment
import com.bicycle.view.settings.SettingsFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton


class MainActivity : AppCompatActivity() {
    companion object {
        lateinit var instance: MainActivity
            private set
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsFab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        instance = this

        // Set a flag to display the activity in fullscreen mode
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        supportFragmentManager.beginTransaction().replace(binding.container.id, MainFragment()).commit()

        supportFragmentManager.addOnBackStackChangedListener { setFabIcon() }

        settingsFab = binding.fabSettings
        setupFabClickListener()
    }

    /**
     * Set the icon on the FAB depending on the current fragment
     */
    private fun setFabIcon() {
        val currentFragment = supportFragmentManager.findFragmentById(binding.container.id)
        if (currentFragment is SettingsFragment) {
            settingsFab.setImageResource(R.drawable.baseline_arrow_back)
        } else {
            settingsFab.setImageResource(android.R.drawable.ic_menu_manage)
        }
    }

    private fun setupFabClickListener() {
        settingsFab.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(binding.container.id)

            if (currentFragment is SettingsFragment) {
                closeSettingsFragment()
            } else {
                showPasswordDialog { openSettingsFragment() }
            }
        }
    }

    private fun openSettingsFragment() {
        supportFragmentManager.beginTransaction()
            .replace(binding.container.id, SettingsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun closeSettingsFragment() {
        supportFragmentManager.popBackStack()
    }

    /**
     * Display the password dialog
     */
    private fun showPasswordDialog(onSuccess: () -> Unit) {
        val password = Const.password

        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.enter_password_title))

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.ok_button_text)) { dialog, _ ->
            val enteredPassword = input.text.toString()
            if (enteredPassword == password) {
                onSuccess()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.incorrect_password_text),
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
        }

        builder.show()
    }
}


