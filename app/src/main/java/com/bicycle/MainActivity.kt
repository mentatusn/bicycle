package com.bicycle

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bicycle.view.MainFragment
import com.bicycle.view.settings.SettingsFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        supportFragmentManager.beginTransaction().replace(R.id.container, MainFragment()).commit()

        val settingsFab: FloatingActionButton = findViewById(R.id.fabSettings)
        settingsFab.setOnClickListener {

            val password = "1234"

            // Создание диалога с запросом пароля
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Введите пароль")

            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            builder.setView(input)

            builder.setPositiveButton("OK") { dialog, _ ->
                val enteredPassword = input.text.toString()

                if (enteredPassword == password) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, SettingsFragment())
                        .addToBackStack(null)
                        .commit()
                } else {
                    Toast.makeText(this, "Неверный пароль", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    //finish()
                }
            }

            builder.show()

        }
    }

}