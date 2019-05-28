package com.example.messenger

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText

class Settings : AppCompatActivity() {
    lateinit var loginText : EditText
    lateinit var loginButton : Button
    var userLogin= String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        loginText = findViewById(R.id.typeLogin)
        loginButton = findViewById(R.id.setLoginButton)
        loginButton.setOnClickListener {
            var user = User
            user.login = loginText.text.toString()
            Log.d("TAG",user.login)
        }
    }




}
